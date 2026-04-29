# Useful command for building/running firmador libre

package:
	mvn clean package

compile:
	mvn clean package -DskipTests

run:
	java -jar target/firmador.jar

build-run: package
	make run

documentation:
	sphinx-build -b html ./docs/source ./docs/build/

clean:
	rm -rf target

background:
	java -jar target/firmador.jar --background

build_to_api:
	mvn clean install -DskipTests -Ddependency-check.skip=true
