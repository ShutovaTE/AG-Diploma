1. Скачать docker (https://www.docker.com/products/docker-desktop/) и minio (https://hub.docker.com/r/minio/minio)
2. Открыть docker 
3. Открыть PowerShell
4. Вписать в PowerShell:
docker run -d --name minio `
  -p 9000:9000 -p 9001:9001 `
  -e "MINIO_ROOT_USER=minioadmin" `
  -e "MINIO_ROOT_PASSWORD=minioadmin" `
  minio/minio server /data --console-address ":9001"
5. Далее вписать в PowerShell:
setx MINIO_ACCESS_KEY "minioadmin"
setx MINIO_SECRET_KEY "minioadmin"
6. Перезапустить ВСЁ (IDE, Tomcat, терминал)
7. Проверить minio.properties в проекте
src/main/resources/minio.properties что-то типа такое должно быть:

minio.endpoint=http://127.0.0.1:9000
minio.accessKey=${MINIO_ACCESS_KEY:minioadmin}
minio.secretKey=${MINIO_SECRET_KEY:minioadmin}
minio.bucket=vag-images


---
8. Создать файл setenv.bat в apache-tomcat-9.0.97\bin\setenv.bat
9. Внести туда 2 и 3 строку тз файла minio.properties
типа такого 
@echo off
set "MINIO_ACCESS_KEY=minioadmin"
set "MINIO_SECRET_KEY=minioadmin"

