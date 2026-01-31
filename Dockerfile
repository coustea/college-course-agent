FROM ubuntu:latest
LABEL authors="couseta"

ENTRYPOINT ["top", "-b"]