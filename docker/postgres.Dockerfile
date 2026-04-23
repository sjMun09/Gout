# pgvector/pgvector:pg17 에 PostGIS 를 얹은 이미지.
# V4__create_hospitals.sql 의 geography(Point, 4326) 컬럼을 쓰기 위해 필요.
# 공식 단일 이미지가 없어 apt 로 postgis 패키지를 설치한다.
FROM pgvector/pgvector:pg17

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      postgresql-17-postgis-3 \
      postgresql-17-postgis-3-scripts \
 && rm -rf /var/lib/apt/lists/*
