# -*- coding: utf-8 -*-
"""
Рекомендательная система для Virtual Art Gallery (VAG)
Работает с реальной базой данных MySQL из CODE_VAG

Использует гибридный подход:
1. Контентная фильтрация (автор + категории)
2. Коллаборативная фильтрация (SVD на основе лайков)
3. Гибридная система (взвешенная комбинация)
"""

import pandas as pd
import numpy as np
from collections import defaultdict
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.model_selection import train_test_split
import mysql.connector
from mysql.connector import Error


# =============================================================================
# 1. Подключение к базе данных MySQL
# =============================================================================

def get_db_connection():
    """Создание подключения к базе данных MySQL"""
    try:
        connection = mysql.connector.connect(
            host='localhost',
            port=3306,
            database='vag1',
            user='root',  # Замените на вашего пользователя
            password='root'  # Замените на ваш пароль
        )
        return connection
    except Error as e:
        print(f"Ошибка подключения к БД: {e}")
        return None


def load_data_from_db(connection):
    """Загрузка всех необходимых данных из базы"""
    
    # Загрузка работ с авторами и категориями
    artworks_query = """
        SELECT 
            a.id AS artwork_id,
            a.title,
            a.description,
            a.date_creation,
            a.likes,
            a.views,
            a.status,
            u.id AS user_id,
            u.username AS author_name
        FROM artworks a
        JOIN users u ON a.user_id = u.id
        WHERE a.status = 'APPROVED'
    """
    artworks_df = pd.read_sql(artworks_query, connection)
    
    # Загрузка категорий для каждой работы
    categories_query = """
        SELECT 
            ac.artwork_id,
            c.name AS category_name
        FROM artwork_category ac
        JOIN categories c ON ac.category_id = c.id
    """
    categories_df = pd.read_sql(categories_query, connection)
    
    # Группировка категорий по работе
    categories_grouped = categories_df.groupby('artwork_id')['category_name'].apply(
        lambda x: ','.join(x)
    ).reset_index()
    categories_grouped.columns = ['artwork_id', 'categories']
    
    artworks_df = artworks_df.merge(categories_grouped, on='artwork_id', how='left')
    artworks_df['categories'] = artworks_df['categories'].fillna('')
    
    # Загрузка лайков (взаимодействия пользователей с работами)
    likes_query = """
        SELECT 
            l.user_id,
            l.artwork_id
        FROM likes l
    """
    likes_df = pd.read_sql(likes_query, connection)
    
    # Загрузка просмотров работ пользователями
    # Примечание: если у вас есть таблица views, раскомментируйте этот блок
    # views_query = """
    #     SELECT 
    #         v.user_id,
    #         v.artwork_id
    #     FROM views v
    # """
    # views_df = pd.read_sql(views_query, connection)
    
    # Загрузка комментариев
    comments_query = """
        SELECT 
            c.user_id,
            c.artwork_id,
            c.date_created
        FROM comments c
    """
    comments_df = pd.read_sql(comments_query, connection)
    
    return artworks_df, likes_df, comments_df


# =============================================================================
# 2. Подготовка данных и создание признаков
# =============================================================================

def prepare_interactions(artworks_df, likes_df, comments_df):
    """
    Создание матрицы взаимодействий:
    - like = 1.0
    - comment = 0.5 (пользователь потратил время на написание)
    """
    interactions = []
    
    # Лайки
    for _, row in likes_df.iterrows():
        interactions.append({
            'user_id': int(row['user_id']),
            'artwork_id': int(row['artwork_id']),
            'like': 1.0,
            'comment': 0.0
        })
    
    # Комментарии (если пользователь комментировал, но не лайкал)
    for _, row in comments_df.iterrows():
        uid = int(row['user_id'])
        aid = int(row['artwork_id'])
        
        # Проверяем, есть ли уже лайк
        already_liked = any(
            (r['user_id'] == uid and r['artwork_id'] == aid) 
            for r in interactions
        )
        
        if not already_liked:
            interactions.append({
                'user_id': uid,
                'artwork_id': aid,
                'like': 0.0,
                'comment': 1.0
            })
    
    interactions_df = pd.DataFrame(interactions)
    
    if interactions_df.empty:
        # Если нет взаимодействий, создаём пустой DataFrame с нужными колонками
        return pd.DataFrame(columns=['user_id', 'artwork_id', 'like', 'comment', 'rating'])
    
    # Составной рейтинг: лайк = 1.0, комментарий = 0.5
    interactions_df['rating'] = interactions_df['like'] * 1.0 + interactions_df['comment'] * 0.5
    
    return interactions_df


def build_content_features(artworks_df):
    """Создание TF-IDF признаков для контентной фильтрации"""
    
    # Текстовое представление: автор + категории
    artworks_df['features'] = (
        artworks_df['author_name'] + ' ' + artworks_df['categories']
    )
    
    tfidf = TfidfVectorizer(token_pattern=r'(?u)\b\w+\b')
    feature_matrix = tfidf.fit_transform(artworks_df['features'])
    
    # Матрица косинусного сходства между всеми работами
    content_sim = cosine_similarity(feature_matrix)
    
    return artworks_df, feature_matrix, content_sim, tfidf


# =============================================================================
# 3. Контентная фильтрация
# =============================================================================

def get_content_recommendations(artwork_id, artworks_df, content_sim, top_n=5):
    """
    Рекомендации на основе сходства с заданной работой.
    Возвращает top_n наиболее похожих работ.
    """
    # Находим индекс работы
    artwork_idx = artworks_df[artworks_df['artwork_id'] == artwork_id].index
    
    if len(artwork_idx) == 0:
        return []
    
    idx = artwork_idx[0]
    
    # Получаем оценки сходства
    sim_scores = list(enumerate(content_sim[idx]))
    sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
    
    # Пропускаем саму работу и берём top_n
    sim_scores = sim_scores[1:top_n+1]
    
    return [int(artworks_df.iloc[i[0]]['artwork_id']) for i in sim_scores]


# =============================================================================
# 4. Коллаборативная фильтрация (SVD)
# =============================================================================

class SimpleSVD:
    """
    Реализация SVD для рекомендательных систем через стохастический градиентный спуск.
    Аналог Surprise SVD, но без внешних зависимостей.
    """
    
    def __init__(self, n_factors=10, lr=0.005, reg=0.02, n_epochs=20):
        self.n_factors = n_factors
        self.lr = lr
        self.reg = reg
        self.n_epochs = n_epochs
        self.user_factors = None
        self.item_factors = None
        self.user_bias = None
        self.item_bias = None
        self.global_mean = None
    
    def fit(self, user_ids, item_ids, ratings):
        """Обучение модели на данных взаимодействиях"""
        self.global_mean = np.mean(ratings)
        
        # Размер массивов определяется максимальными ID
        max_user_id = int(np.max(user_ids)) if len(user_ids) > 0 else 0
        max_item_id = int(np.max(item_ids)) if len(item_ids) > 0 else 0
        
        if max_user_id == 0 or max_item_id == 0:
            return  # Нет данных для обучения
        
        # Инициализация факторов и смещений (+1 для 1-based индексации)
        np.random.seed(42)
        self.user_factors = np.random.normal(0, 0.1, (max_user_id + 1, self.n_factors))
        self.item_factors = np.random.normal(0, 0.1, (max_item_id + 1, self.n_factors))
        self.user_bias = np.zeros(max_user_id + 1)
        self.item_bias = np.zeros(max_item_id + 1)
        
        # SGD обучение
        for epoch in range(self.n_epochs):
            indices = np.arange(len(ratings))
            np.random.shuffle(indices)
            
            for idx in indices:
                u = int(user_ids[idx])
                i = int(item_ids[idx])
                r = ratings[idx]
                
                # Предсказание
                pred = self.global_mean + self.user_bias[u] + self.item_bias[i] + \
                       np.dot(self.user_factors[u], self.item_factors[i])
                
                # Ошибка
                err = r - pred
                
                # Обновление параметров
                self.user_bias[u] += self.lr * (err - self.reg * self.user_bias[u])
                self.item_bias[i] += self.lr * (err - self.reg * self.item_bias[i])
                
                old_uf = self.user_factors[u].copy()
                self.user_factors[u] += self.lr * (err * self.item_factors[i] - self.reg * self.user_factors[u])
                self.item_factors[i] += self.lr * (err * old_uf - self.reg * self.item_factors[i])
    
    def predict(self, user_id, item_id):
        """Предсказание рейтинга для пары пользователь-работа"""
        if self.global_mean is None:
            return 0.0
        
        pred = self.global_mean + self.user_bias[user_id] + self.item_bias[item_id] + \
               np.dot(self.user_factors[user_id], self.item_factors[item_id])
        
        return max(0.0, min(pred, 1.5))  # Ограничение диапазона


def train_svd_model(interactions_df):
    """Обучение SVD модели на всех данных взаимодействий"""
    
    if interactions_df.empty:
        print("Нет данных взаимодействий для обучения SVD")
        return None, None, None
    
    user_ids = interactions_df['user_id'].values.astype(int)
    item_ids = interactions_df['artwork_id'].values.astype(int)
    ratings = interactions_df['rating'].values
    
    # Разделение на train/test для оценки качества
    train_idx, test_idx = train_test_split(
        np.arange(len(interactions_df)), test_size=0.25, random_state=42
    )
    
    train_users = user_ids[train_idx]
    train_items = item_ids[train_idx]
    train_ratings = ratings[train_idx]
    
    test_users = user_ids[test_idx]
    test_items = item_ids[test_idx]
    test_ratings = ratings[test_idx]
    
    # Обучение SVD
    svd = SimpleSVD(n_factors=10, lr=0.005, reg=0.02, n_epochs=20)
    svd.fit(train_users, train_items, train_ratings)
    
    return svd, (test_users, test_items, test_ratings), (train_users, train_items, train_ratings)


# =============================================================================
# 5. Гибридная система рекомендаций
# =============================================================================

def get_collab_recommendations(user_id, all_artwork_ids, svd, top_n=5):
    """
    Рекомендации на основе коллаборативной фильтрации.
    Возвращает top_n работ с наивысшими предсказанными рейтингами.
    """
    predictions = []
    for aid in all_artwork_ids:
        pred = svd.predict(user_id, int(aid))
        predictions.append((aid, pred))
    
    predictions.sort(key=lambda x: x[1], reverse=True)
    return [p[0] for p in predictions[:top_n]]


def hybrid_recommendations(user_id, interactions_df, artworks_df, svd, 
                           content_sim, alpha=0.6, top_n=5):
    """
    Гибридные рекомендации: комбинация контентной и коллаборативной фильтрации.
    
    Параметры:
    - user_id: ID пользователя
    - interactions_df: DataFrame взаимодействий
    - artworks_df: DataFrame работ
    - svd: обученная SVD модель
    - content_sim: матрица косинусного сходства
    - alpha: вес коллаборативной составляющей (0.0 - 1.0)
    - top_n: количество рекомендаций
    
    Возвращает: список ID рекомендованных работ
    """
    
    # Работы, с которыми пользователь уже взаимодействовал
    if not interactions_df.empty:
        seen = interactions_df[interactions_df['user_id'] == user_id]['artwork_id'].tolist()
    else:
        seen = []
    
    # Кандидаты на рекомендацию (все работы минус просмотренные)
    candidates = [aid for aid in artworks_df['artwork_id'] if aid not in seen]
    
    if not candidates:
        return []
    
    # --- Коллаборативные предсказания ---
    collab_scores = {}
    for aid in candidates:
        pred = svd.predict(user_id, int(aid))
        collab_scores[aid] = pred
    
    # --- Контентные предсказания ---
    # Находим работы, которые пользователь лайкал
    if not interactions_df.empty:
        liked = interactions_df[
            (interactions_df['user_id'] == user_id) & 
            (interactions_df['like'] > 0)
        ]['artwork_id'].tolist()
    else:
        liked = []
    
    content_scores = {}
    for aid in candidates:
        scores = []
        for liked_id in liked:
            liked_idx = artworks_df[artworks_df['artwork_id'] == liked_id].index
            candidate_idx = artworks_df[artworks_df['artwork_id'] == aid].index
            
            if len(liked_idx) > 0 and len(candidate_idx) > 0:
                scores.append(content_sim[liked_idx[0]][candidate_idx[0]])
        
        content_scores[aid] = np.mean(scores) if scores else 0
    
    # --- Нормализация оценок к диапазону [0,1] ---
    max_collab = max(collab_scores.values()) if collab_scores else 1
    max_content = max(content_scores.values()) if content_scores else 1
    
    hybrid = {}
    for aid in candidates:
        collab_norm = collab_scores[aid] / max_collab if max_collab > 0 else 0
        content_norm = content_scores[aid] / max_content if max_content > 0 else 0
        hybrid[aid] = alpha * collab_norm + (1 - alpha) * content_norm
    
    # --- Сортировка и возврат top_n ---
    sorted_items = sorted(hybrid.items(), key=lambda x: x[1], reverse=True)
    return [int(aid) for aid, _ in sorted_items[:top_n]]


# =============================================================================
# 6. Оценка качества рекомендаций
# =============================================================================

def precision_recall_at_k(predictions, k=5, threshold=1.0):
    """
    Расчёт Precision@K и Recall@K.
    Релевантным считается произведение с истинным рейтингом >= threshold.
    """
    user_est_true = defaultdict(list)
    for uid, iid, true_r, est, _ in predictions:
        user_est_true[uid].append((est, true_r))
    
    precisions = []
    recalls = []
    
    for uid, user_ratings in user_est_true.items():
        user_ratings.sort(key=lambda x: x[0], reverse=True)
        top_k = user_ratings[:k]
        
        relevant = sum(1 for (_, true_r) in user_ratings if true_r >= threshold)
        retrieved = sum(1 for (_, true_r) in top_k if true_r >= threshold)
        
        precisions.append(retrieved / k if k > 0 else 0)
        recalls.append(retrieved / relevant if relevant > 0 else 0)
    
    return np.mean(precisions), np.mean(recalls)


def coverage(predictions, all_item_ids, k=5):
    """
    Доля уникальных произведений, попавших в top-K рекомендаций.
    Показывает, насколько разнообразны рекомендации.
    """
    recommended_items = set()
    user_predictions = defaultdict(list)
    
    for uid, iid, _, est, _ in predictions:
        user_predictions[uid].append((iid, est))
    
    for uid, items in user_predictions.items():
        items.sort(key=lambda x: x[1], reverse=True)
        top_k_items = [iid for iid, _ in items[:k]]
        recommended_items.update(top_k_items)
    
    return len(recommended_items) / len(all_item_ids) if len(all_item_ids) > 0 else 0


def evaluate_model(svd, test_data, artworks_df):
    """Оценка качества модели на тестовых данных"""
    
    if test_data is None:
        print("Нет тестовых данных для оценки")
        return
    
    test_users, test_items, test_ratings = test_data
    
    # Создаём предсказания
    test_predictions = []
    for u, i, r in zip(test_users, test_items, test_ratings):
        pred = svd.predict(u, i)
        test_predictions.append((u, i, r, pred, None))
    
    # Метрики
    k = 5
    precision, recall = precision_recall_at_k(test_predictions, k=k, threshold=1.0)
    cov = coverage(test_predictions, artworks_df['artwork_id'].tolist(), k=k)
    
    print(f"\n{'='*50}")
    print(f"Оценка качества модели")
    print(f"{'='*50}")
    print(f"Precision@{k} = {precision:.3f}")
    print(f"Recall@{k}    = {recall:.3f}")
    print(f"Coverage      = {cov:.3f}")
    print(f"{'='*50}")


# =============================================================================
# 7. Вспомогательные функции для интеграции с Java
# =============================================================================

def get_recommendations_for_user_json(user_id, connection, top_n=10):
    """
    Функция для вызова из Java-приложения.
    Возвращает JSON-совместимый словарь с рекомендациями.
    
    Использование в Java:
    - Вызвать как subprocess: python recommendation_engine.py --user_id 1
    - Распарсить вывод JSON
    """
    import json
    
    # Загрузка данных
    artworks_df, likes_df, comments_df = load_data_from_db(connection)
    
    if artworks_df.empty:
        return json.dumps({"error": "Нет данных в базе", "recommendations": []})
    
    # Подготовка взаимодействий
    interactions_df = prepare_interactions(artworks_df, likes_df, comments_df)
    
    # Контентные признаки
    artworks_df, _, content_sim, _ = build_content_features(artworks_df)
    
    # Обучение SVD
    svd, test_data, _ = train_svd_model(interactions_df)
    
    if svd is None:
        # Если нет данных для SVD, используем только контентную фильтрацию
        recommendations = []
        for _, row in artworks_df.iterrows():
            recommendations.append({
                "artwork_id": int(row['artwork_id']),
                "title": row['title'],
                "author": row['author_name'],
                "categories": row['categories'],
                "score": 0.5
            })
        return json.dumps({
            "user_id": user_id,
            "recommendations": recommendations[:top_n],
            "algorithm": "content_only"
        })
    
    # Гибридные рекомендации
    rec_ids = hybrid_recommendations(
        user_id, interactions_df, artworks_df, svd, 
        content_sim, alpha=0.6, top_n=top_n
    )
    
    # Формирование ответа с деталями работ
    recommendations = []
    for aid in rec_ids:
        artwork = artworks_df[artworks_df['artwork_id'] == aid]
        if not artwork.empty:
            row = artwork.iloc[0]
            recommendations.append({
                "artwork_id": int(row['artwork_id']),
                "title": row['title'],
                "author": row['author_name'],
                "categories": row['categories'],
                "likes": int(row['likes']),
                "score": 0.8  # Можно заменить на реальный score
            })
    
    return json.dumps({
        "user_id": user_id,
        "recommendations": recommendations,
        "algorithm": "hybrid"
    })


# =============================================================================
# 8. Главная функция (демонстрация работы)
# =============================================================================

def main():
    """Демонстрация работы рекомендательной системы"""
    
    print("\n" + "="*60)
    print("  Рекомендательная система Virtual Art Gallery")
    print("="*60 + "\n")
    
    # Подключение к БД
    connection = get_db_connection()
    if connection is None:
        print("Не удалось подключиться к базе данных.")
        print("Проверьте параметры подключения в функции get_db_connection()")
        return
    
    try:
        # Загрузка данных
        print("Загрузка данных из базы...")
        artworks_df, likes_df, comments_df = load_data_from_db(connection)
        
        print(f"  Загружено работ: {len(artworks_df)}")
        print(f"  Загружено лайков: {len(likes_df)}")
        print(f"  Загружено комментариев: {len(comments_df)}")
        
        if artworks_df.empty:
            print("\nНет одобренных работ в базе. Завершение.")
            return
        
        # Подготовка взаимодействий
        print("\nПодготовка взаимодействий...")
        interactions_df = prepare_interactions(artworks_df, likes_df, comments_df)
        print(f"  Всего взаимодействий: {len(interactions_df)}")
        
        # Контентная фильтрация
        print("\nПостроение контентных признаков...")
        artworks_df, _, content_sim, _ = build_content_features(artworks_df)
        print(f"  Матрица признаков: {content_sim.shape}")
        
        # Обучение SVD
        print("\nОбучение SVD модели...")
        svd, test_data, train_data = train_svd_model(interactions_df)
        
        if svd is not None:
            print("  SVD модель обучена успешно")
            evaluate_model(svd, test_data, artworks_df)
        else:
            print("  SVD модель не обучена (недостаточно данных)")
        
        # Пример рекомендаций для пользователя
        if not interactions_df.empty:
            user_id = interactions_df['user_id'].iloc[0]
            
            print(f"\n{'='*60}")
            print(f"  Рекомендации для пользователя {user_id}")
            print(f"{'='*60}")
            
            rec_ids = hybrid_recommendations(
                user_id, interactions_df, artworks_df, svd, 
                content_sim, alpha=0.6, top_n=5
            )
            
            print("\nТоп-5 рекомендованных работ:")
            for i, aid in enumerate(rec_ids, 1):
                artwork = artworks_df[artworks_df['artwork_id'] == aid]
                if not artwork.empty:
                    row = artwork.iloc[0]
                    print(f"  {i}. ID {aid}: {row['title']} — {row['author_name']}")
                    if row['categories']:
                        print(f"     Категории: {row['categories']}")
        
        # Контентные рекомендации для случайной работы
        if len(artworks_df) > 0:
            sample_artwork_id = artworks_df.iloc[0]['artwork_id']
            
            print(f"\n{'='*60}")
            print(f"  Похожие работы для работы ID {sample_artwork_id}")
            print(f"{'='*60}")
            
            sample_artwork = artworks_df[artworks_df['artwork_id'] == sample_artwork_id].iloc[0]
            print(f"\nИсходная работа: {sample_artwork['title']} — {sample_artwork['author_name']}")
            print(f"Категории: {sample_artwork['categories']}")
            
            similar_ids = get_content_recommendations(sample_artwork_id, artworks_df, content_sim, top_n=3)
            
            print("\nПохожие работы:")
            for i, aid in enumerate(similar_ids, 1):
                artwork = artworks_df[artworks_df['artwork_id'] == aid]
                if not artwork.empty:
                    row = artwork.iloc[0]
                    print(f"  {i}. ID {aid}: {row['title']} — {row['author_name']}")
        
        # Демонстрация JSON вывода для интеграции с Java
        print(f"\n{'='*60}")
        print("  JSON вывод для интеграции с Java")
        print(f"{'='*60}")
        
        if not interactions_df.empty:
            user_id = interactions_df['user_id'].iloc[0]
            json_output = get_recommendations_for_user_json(user_id, connection, top_n=3)
            print(json_output)
        
    finally:
        if connection.is_connected():
            connection.close()
            print("\n\nПодключение к БД закрыто.")


# =============================================================================
# 9. Запуск из командной строки
# =============================================================================

if __name__ == "__main__":
    import sys
    
    # Проверка аргументов командной строки
    if len(sys.argv) > 1 and sys.argv[1] == "--user_id":
        # Режим для вызова из Java
        user_id = int(sys.argv[2])
        connection = get_db_connection()
        if connection:
            print(get_recommendations_for_user_json(user_id, connection, top_n=10))
            connection.close()
    else:
        # Режим демонстрации
        main()
