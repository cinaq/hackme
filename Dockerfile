FROM cinaq/mxbuild-docker:latest AS builder
COPY . /srv/mendix/package
RUN mendix-build

FROM cinaq/mendix-docker:latest
# runtime is always needed to run
RUN mendix-download 8.13.0.3768
COPY --from=builder /srv/mendix/data/model-upload/app.mda /srv/mendix/data/model-upload/app.mda
RUN mendix-unpack
