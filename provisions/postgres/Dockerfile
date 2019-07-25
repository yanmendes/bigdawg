FROM wtanaka/ubuntu-1604-oracle-java-8

# Install utils

RUN \
	apt-get update && \
	apt-get install -y curl && \
	apt-get install -y netcat && \
	apt-get install -y iputils-ping && \
	apt-get install -y net-tools && \
	apt-get install wget

# Install postgres 9.4
RUN echo "deb http://apt.postgresql.org/pub/repos/apt/ trusty-pgdg main" > /etc/apt/sources.list.d/pgdg.list  && \
	wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - && \
	apt-get update && \
	apt-get install -y postgresql-9.4 && \
	psql --version && \
	apt-get update && \
	apt-get install -y software-properties-common postgresql-9.4 postgresql-client-9.4 postgresql-contrib-9.4 && \
	chmod -R 0700 /etc/ssl/private && \
	chown -R postgres /etc/ssl/private


RUN rm /etc/postgresql/9.4/main/pg_hba.conf && \
	echo "host all all all trust" >> /etc/postgresql/9.4/main/pg_hba.conf && \
	echo "local all all trust" >> /etc/postgresql/9.4/main/pg_hba.conf && \
	chmod 777 /etc/postgresql/9.4/main/pg_hba.conf && \
	ls -l /etc/postgresql/9.4/main/pg_hba.conf

# Configure postgres
USER postgres
RUN /etc/init.d/postgresql start &&\
	psql -c "ALTER USER postgres PASSWORD 'test';" && \
	echo "listen_addresses='*'" >> /etc/postgresql/9.4/main/postgresql.conf && \
	psql -c "CREATE USER pguser WITH SUPERUSER CREATEDB LOGIN REPLICATION PASSWORD 'test';" && \
	/etc/init.d/postgresql stop

COPY start_services.sh /

# Copy over build artifacts
# Getting ParserTerms: cp src/main/resources/{PostgresParserTerms.csv,SciDBParserTerms.csv} provisions/postgres/
# Getting jar: mvn package -P mit -DskipTests -f pom.xml && cp target/istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar provisions/postgres/
COPY PostgresParserTerms.csv /src/main/resources/PostgresParserTerms.csv
COPY SciDBParserTerms.csv /src/main/resources/SciDBParserTerms.csv
COPY istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar /

CMD /start_services.sh