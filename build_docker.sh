
./gradlew clean build

docker build --label "local" \
        --tag "tbk/cln-spend-and-replace" .

