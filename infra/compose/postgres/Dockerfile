FROM postgres:16-trixie

# Keep legacy timezone names such as Asia/Saigon available to clients.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata-legacy \
    && rm -rf /var/lib/apt/lists/*
