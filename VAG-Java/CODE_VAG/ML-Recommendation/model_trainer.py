# -*- coding: utf-8 -*-
"""
Скрипт отдельного обучения модели рекомендательной системы VAG
Загружает данные, обучает модель и сохраняет её отдельно

Запуск: python model_trainer.py [--force]
"""

import sys
import os
import json
import pickle
import pandas as pd
import numpy as np
from datetime import datetime
import mysql.connector
from mysql.connector import Error
import warnings
warnings.filterwarnings('ignore')

# Импортируем функции из основного скрипта рекомендаций
from recommendation_engine import (
    get_db_connection, load_data_from_db, prepare_interactions,
    build_content_features, train_svd_model, evaluate_model,
    SimpleSVD
)

# === КОНФИГУРАЦИЯ ===
CACHE_DIR = os.path.join(os.path.dirname(__file__), 'model_cache')
CACHE_FILE = os.path.join(CACHE_DIR, 'recommendation_model.pkl')
METADATA_FILE = os.path.join(CACHE_DIR, 'model_metadata.json')
LOG_FILE = os.path.join(CACHE_DIR, 'training.log')
BACKUP_DIR = os.path.join(CACHE_DIR, 'backups')


def ensure_cache_dir():
    """Создание необходимых директорий"""
    if not os.path.exists(CACHE_DIR):
        os.makedirs(CACHE_DIR)
    if not os.path.exists(BACKUP_DIR):
        os.makedirs(BACKUP_DIR)


def log_message(message, level='INFO'):
    """Логирование сообщений в файл и консоль"""
    ensure_cache_dir()
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    log_entry = f"[{timestamp}] [{level}] {message}"
    
    print(log_entry)
    
    with open(LOG_FILE, 'a', encoding='utf-8') as f:
        f.write(log_entry + '\n')


def backup_old_model():
    """Резервная копия старой модели перед переобучением"""
    if os.path.exists(CACHE_FILE):
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_file = os.path.join(BACKUP_DIR, f'recommendation_model_{timestamp}.pkl')
        try:
            import shutil
            shutil.copy2(CACHE_FILE, backup_file)
            log_message(f"Создана резервная копия модели: {backup_file}")
        except Exception as e:
            log_message(f"Ошибка при создании резервной копии: {e}", 'ERROR')


def save_metadata(training_stats):
    """Сохранение метаданных обучения модели"""
    ensure_cache_dir()
    
    metadata = {
        'version': '2.0',
        'training_date': datetime.now().isoformat(),
        'training_duration_seconds': training_stats.get('duration', 0),
        'data_statistics': training_stats.get('data_stats', {}),
        'model_quality': training_stats.get('quality_metrics', {}),
        'training_parameters': training_stats.get('parameters', {})
    }
    
    with open(METADATA_FILE, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)
    
    log_message(f"Метаданные модели сохранены: {METADATA_FILE}")


def load_metadata():
    """Загрузка метаданных модели"""
    if os.path.exists(METADATA_FILE):
        try:
            with open(METADATA_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            log_message(f"Ошибка при загрузке метаданных: {e}", 'ERROR')
    return None


def train_and_save_model(force=False):
    """
    Основная функция обучения и сохранения модели
    
    Параметры:
        force (bool): Если True, переобучать модель без проверки
    
    Возвращает:
        dict: Статистика обучения или None если произошла ошибка
    """
    import time
    
    log_message("=" * 70)
    log_message("Начало обучения модели рекомендаций")
    log_message("=" * 70)
    
    start_time = time.time()
    stats = {
        'duration': 0,
        'data_stats': {},
        'quality_metrics': {},
        'parameters': {},
        'success': False,
        'error': None
    }
    
    try:
        # === ШАГ 1: Подключение к БД ===
        log_message("Подключение к базе данных...")
        connection = get_db_connection()
        if connection is None:
            raise Exception("Не удалось подключиться к БД")
        
        log_message("✓ Подключение к БД успешно")
        
        # === ШАГ 2: Загрузка данных ===
        log_message("Загрузка данных из БД...")
        artworks_df, likes_df, comments_df = load_data_from_db(connection)
        connection.close()
        
        stats['data_stats'] = {
            'artworks_count': len(artworks_df),
            'likes_count': len(likes_df),
            'comments_count': len(comments_df)
        }
        
        log_message(f"✓ Загружено работ: {len(artworks_df)}")
        log_message(f"✓ Загружено лайков: {len(likes_df)}")
        log_message(f"✓ Загружено комментариев: {len(comments_df)}")
        
        if artworks_df.empty:
            raise Exception("Нет одобренных работ в БД")
        
        # === ШАГ 3: Подготовка взаимодействий ===
        log_message("Подготовка матрицы взаимодействий...")
        interactions_df = prepare_interactions(artworks_df, likes_df, comments_df)
        log_message(f"✓ Всего взаимодействий: {len(interactions_df)}")
        
        # === ШАГ 4: Построение контентных признаков ===
        log_message("Построение контентных признаков (TF-IDF)...")
        artworks_df, _, content_sim, _ = build_content_features(artworks_df)
        log_message(f"✓ Матрица сходства: {content_sim.shape}")
        
        # === ШАГ 5: Обучение SVD модели ===
        log_message("Обучение SVD модели (это может занять время)...")
        svd, test_data, train_data = train_svd_model(interactions_df)
        
        if svd is None:
            log_message("⚠ SVD модель не была обучена (недостаточно данных)")
        else:
            log_message("✓ SVD модель обучена успешно")
        
        # === ШАГ 6: Оценка качества ===
        log_message("Оценка качества модели...")
        if test_data is not None and svd is not None:
            # Парсим результаты оценки
            test_users, test_items, test_ratings = test_data
            test_predictions = []
            for u, i, r in zip(test_users, test_items, test_ratings):
                pred = svd.predict(u, i)
                test_predictions.append((u, i, r, pred, None))
            
            from recommendation_engine import precision_recall_at_k, coverage
            
            k = 5
            precision, recall = precision_recall_at_k(test_predictions, k=k, threshold=1.0)
            cov = coverage(test_predictions, artworks_df['artwork_id'].tolist(), k=k)
            
            stats['quality_metrics'] = {
                'precision_at_5': float(precision),
                'recall_at_5': float(recall),
                'coverage_at_5': float(cov)
            }
            
            log_message(f"✓ Precision@5: {precision:.3f}")
            log_message(f"✓ Recall@5: {recall:.3f}")
            log_message(f"✓ Coverage@5: {cov:.3f}")
        
        # === ШАГ 7: Создание резервной копии старой модели ===
        log_message("Создание резервной копии старой модели...")
        backup_old_model()
        
        # === ШАГ 8: Сохранение новой модели ===
        log_message("Сохранение обученной модели...")
        ensure_cache_dir()
        
        model_data = {
            'svd': svd,
            'content_sim': content_sim,
            'artworks_df': artworks_df,
            'interactions_df': interactions_df
        }
        
        with open(CACHE_FILE, 'wb') as f:
            pickle.dump(model_data, f)
        
        log_message(f"✓ Модель сохранена: {CACHE_FILE}")
        
        # === ШАГ 9: Сохранение метаданных ===
        stats['parameters'] = {
            'n_factors': 10,
            'learning_rate': 0.005,
            'regularization': 0.02,
            'n_epochs': 20,
            'alpha_hybrid': 0.6
        }
        
        duration = time.time() - start_time
        stats['duration'] = duration
        stats['success'] = True
        
        save_metadata(stats)
        
        # === Итоговое сообщение ===
        log_message("=" * 70)
        log_message(f"✓ ОБУЧЕНИЕ ЗАВЕРШЕНО УСПЕШНО за {duration:.1f} сек")
        log_message("=" * 70)
        
        return stats
        
    except Exception as e:
        stats['success'] = False
        stats['error'] = str(e)
        stats['duration'] = time.time() - start_time
        
        log_message(f"✗ ОШИБКА при обучении: {e}", 'ERROR')
        log_message("=" * 70)
        
        return stats


def print_model_info():
    """Вывод информации о текущей модели"""
    print("\n" + "=" * 70)
    print("ИНФОРМАЦИЯ О ТЕКУЩЕЙ МОДЕЛИ")
    print("=" * 70)
    
    if os.path.exists(CACHE_FILE):
        file_size_mb = os.path.getsize(CACHE_FILE) / (1024 * 1024)
        print(f"✓ Модель существует: {CACHE_FILE}")
        print(f"  Размер: {file_size_mb:.2f} МБ")
    else:
        print("✗ Модель не найдена")
    
    metadata = load_metadata()
    if metadata:
        print(f"\n✓ Метаданные:")
        print(f"  Версия: {metadata.get('version', 'unknown')}")
        print(f"  Дата обучения: {metadata.get('training_date', 'unknown')}")
        print(f"  Длительность: {metadata.get('training_duration_seconds', 0):.1f} сек")
        
        if metadata.get('data_statistics'):
            print(f"\n✓ Статистика данных:")
            for key, value in metadata['data_statistics'].items():
                print(f"  {key}: {value}")
        
        if metadata.get('model_quality'):
            print(f"\n✓ Качество модели:")
            for key, value in metadata['model_quality'].items():
                print(f"  {key}: {value:.3f}")
    else:
        print("✗ Метаданные не найдены")
    
    print("=" * 70 + "\n")


def main():
    """Главная функция скрипта"""
    force = '--force' in sys.argv
    
    if '--info' in sys.argv:
        print_model_info()
        return
    
    if force:
        log_message("Флаг --force активирован. Переобучение модели.")
    else:
        log_message("Начало обучения модели (используйте --force для принудительного переобучения)")
    
    stats = train_and_save_model(force=force)
    
    if not stats.get('success'):
        sys.exit(1)


if __name__ == "__main__":
    main()
