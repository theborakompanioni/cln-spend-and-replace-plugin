FROM polarlightning/clightning:23.08

RUN apt-get update -y \
  && apt-get install -y openjdk-17-jre \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/

COPY --chmod=0755 cln-snr-plugin/cln-snr-app/build/libs/*-boot.jar /opt/spend-and-replace/bin/
RUN ln -s /opt/spend-and-replace/bin/*-boot.jar /usr/local/libexec/c-lightning/plugins/spend-and-replace \
  && echo "plugin=/usr/local/libexec/c-lightning/plugins/spend-and-replace" > /root/.lightning/config

ENTRYPOINT ["/entrypoint.sh"]

CMD ["lightningd"]

