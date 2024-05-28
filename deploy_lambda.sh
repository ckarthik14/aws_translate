#!/bin/sh

./gradlew clean buildZip --rerun-tasks

aws s3 cp ./build/distributions/aws_translate-1.0-SNAPSHOT.zip s3://aws-real-time-translation/ics_showcase_translate_lambda.zip

aws lambda update-function-code --function-name ics_showcase_translate_lambda --s3-bucket aws-real-time-translation --s3-key ics_showcase_translate_lambda.zip