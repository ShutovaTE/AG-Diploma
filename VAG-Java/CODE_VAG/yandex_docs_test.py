import urllib.request
urls = ['https://cloud.yandex.com/en/docs/vision/req/classification','https://cloud.yandex.com/en/docs/vision/req/request','https://cloud.yandex.com/en/docs/vision']
for url in urls:
    try:
        print('URL:', url)
        req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as r:
            data = r.read(2000).decode('utf-8', 'ignore')
            print(data[:1000])
    except Exception as e:
        print('ERR', url, e)
