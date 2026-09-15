# DAF Committee Agendas remote: static Angular build (remoteEntry.json + chunks) served by the
# generic nginx base. Environment-specific values are NOT built in — the nginx template renders
# assets/config.json from DAF_API_URL at container start.
FROM crsk-docker-private.rmb.co.za/base/nginx:1.29.0

ENV DAF_API_URL=https://credit-systems-dev.rmb.co.za/gateway/daf

COPY dist/daf-ui/browser/ /usr/share/nginx/html/

RUN rm -f /etc/nginx/conf.d/default.conf /usr/share/nginx/html/assets/config.json
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
