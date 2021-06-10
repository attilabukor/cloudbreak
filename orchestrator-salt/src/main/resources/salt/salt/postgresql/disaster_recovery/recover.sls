{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

{% if 'None' != configure_remote_db %}

/opt/salt/scripts/recover_db_remote.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/recover_db_remote.sh
    - template: jinja

recover-services-db-remote:
  cmd.run:
    - name: runuser -l postgres -c '/opt/salt/scripts/recover_db_remote.sh' && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/recover-services-db-remote-executed
    - unless: test -f /var/log/recover-services-db-remote-executed
    - require:
      - file: /opt/salt/scripts/recover_db_remote.sh
{% if postgresql.ssl_enabled == True %}
      - file: {{ postgresql.root_certs_file }}
{%- endif %}

{% endif %}


