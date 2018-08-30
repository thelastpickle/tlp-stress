
FROM ubuntu

RUN apt-get update \
    && apt-get install -y gnupg \
    && apt-get update \
    && apt-get install -y \
        curl \
    && curl -sL https://deb.nodesource.com/setup_6.x -o nodesource_setup.sh \
    && bash nodesource_setup.sh \
    && apt-get update \
    && apt-get install -y \
        build-essential \
        git \
        rpm \
        ruby-dev \
    && gem install fpm

# Define working directory.
WORKDIR /data

# Define default command.
CMD ["bash"]