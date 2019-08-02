ARG OS=ubuntu
ARG FLAVOR=bionic

FROM ${OS}:${FLAVOR}

ARG OS=ubuntu
ARG FLAVOR=bionic

RUN apt-get update && \
    apt-get install -y git debhelper maven openjdk-11-jdk cmake libboost-test-dev libyaml-dev libjemalloc-dev flex bison `if [ "$BASE_IMAGE" = "debian:buster" ]; then echo clang-7 llvm-7-tools lld-7; else echo clang-8 llvm-8-tools lld-8; fi` zlib1g-dev libgmp-dev libmpfr-dev gcc z3 libz3-dev opam pkg-config curl python3
RUN curl -sSL https://get.haskellstack.org/ | sh      

ARG USER_ID=1000
ARG GROUP_ID=1000
RUN groupadd -g $GROUP_ID user && \
    useradd -m -u $USER_ID -s /bin/sh -g user user

USER $USER_ID:$GROUP_ID

ADD llvm-backend/src/main/native/llvm-backend/install-rust llvm-backend/src/main/native/llvm-backend/rust-checksum /home/user/
RUN cd /home/user/ && ./install-rust

ADD k-distribution/src/main/scripts/bin/k-configure-opam-dev k-distribution/src/main/scripts/bin/k-configure-opam-common /home/user/.tmp-opam/bin/
ADD k-distribution/src/main/scripts/lib/opam  /home/user/.tmp-opam/lib/opam/
RUN    cd /home/user \
    && ./.tmp-opam/bin/k-configure-opam-dev

ENV LC_ALL=C.UTF-8
ADD --chown=user:user haskell-backend/src/main/native/haskell-backend/stack.yaml /home/user/.tmp-haskell/
ADD --chown=user:user haskell-backend/src/main/native/haskell-backend/kore/package.yaml /home/user/.tmp-haskell/kore/
RUN    cd /home/user/.tmp-haskell \
    && stack build --only-snapshot

ADD pom.xml /home/user/.tmp-maven/
ADD ktree/pom.xml /home/user/.tmp-maven/ktree/
ADD llvm-backend/pom.xml /home/user/.tmp-maven/llvm-backend/
ADD llvm-backend/src/main/native/llvm-backend/matching/pom.xml /home/user/.tmp-maven/llvm-backend/src/main/native/llvm-backend/matching/
ADD haskell-backend/pom.xml /home/user/.tmp-maven/haskell-backend/
ADD ocaml-backend/pom.xml /home/user/.tmp-maven/ocaml-backend/
ADD kernel/pom.xml /home/user/.tmp-maven/kernel/
ADD java-backend/pom.xml /home/user/.tmp-maven/java-backend/
ADD k-distribution/pom.xml /home/user/.tmp-maven/k-distribution/
ADD kore/pom.xml /home/user/.tmp-maven/kore/
RUN    cd /home/user/.tmp-maven \
    && mvn dependency:go-offline 