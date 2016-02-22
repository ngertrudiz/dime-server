GRADLE   = ./gradlew -q
TARGET   = build/libs/dime-server.jar
JAVADOC_DIR = build/docs/javadoc/
JAVADOC_WEB = shell.hiit.fi:/group/reknow/public_html/javadoc/dime-server/
PKG_DIR  = build/package
PKG_FILE = dime
DIME_PORT = $(shell test -f config/application-local.properties && ((grep '^server.port=' config/application-local.properties || echo server.port=8080) | sed 's/.*port=//') || echo 8080)
SOURCES := $(shell find src/ -name '[A-Z]*.java' -or -name '*.html' -or -name 'db*.xml' -or -name '*.properties') build.gradle

DIME_HOME = ~/.dime

DOCKER_DIR = $(DIME_HOME)/docker
DB_FILE = $(DIME_HOME)/database/h2.mv.db
AUTOGEN_TMP_DIR = $(DIME_HOME)/tmp/autogen

LOG_HEAD = "[MAKE]"

all:	assemble

assemble:  $(TARGET)

$(TARGET): $(SOURCES)
	$(GRADLE) assemble

run:    $(TARGET)
	@lsof -i :$(DIME_PORT) -sTCP:LISTEN && echo '\nERROR: DiMe already running in port $(DIME_PORT)!' || java -jar $(TARGET)

test:
	rm -rf ./build/reports/tests/
	$(GRADLE) test
	@echo $(LOG_HEAD) Now open ./build/reports/tests/index.html

testone:
	$(GRADLE) test --tests fi.hiit.dime.DataControllerTest.testNewTags

clean:
	$(GRADLE) clean

doc: $(SOURCES)
	$(GRADLE) javadoc
	chmod -R a+r $(JAVADOC_DIR)
	rsync -var $(JAVADOC_DIR) $(JAVADOC_WEB)
	@echo
	@echo $(LOG_HEAD) Now open ./build/docs/javadoc/index.html

docker: $(TARGET)
	docker build -t dime-server .

docker_dir:
	@echo $(LOG_HEAD) Creating directory for database ...
	mkdir -p $(DOCKER_DIR)
	chmod 777 $(DOCKER_DIR)

runDocker: docker docker_dir
	docker run -it -p 8080:8080 -v $(DOCKER_DIR):/var/lib/dime dime-server

autogenDb:
	@echo $(LOG_HEAD) Creating temporary database auto-generated by hibernate
	rm -rf $(AUTOGEN_TMP_DIR)
	@echo $(LOG_HEAD) Running tests to auto-generate database
	SPRING_PROFILES_ACTIVE=autogen $(GRADLE) test
#	SPRING_PROFILES_ACTIVE=autogen $(GRADLE) test --tests fi.hiit.dime.ApiControllerTest.testPing

initSchema: autogenDb
	rm -f src/main/resources/db/changelog/db.changelog-master.xml
	$(GRADLE) generateChangelog

updateSchema: $(DB_FILE) autogenDb 
	$(GRADLE) diffChangeLog

package: $(TARGET)
	rm -rf $(PKG_DIR)/$(PKG_FILE)
	mkdir -p $(PKG_DIR)/$(PKG_FILE)
	rsync -var --exclude "*~" scripts/package/* $(PKG_DIR)/$(PKG_FILE)/
	cp $(TARGET) $(PKG_DIR)/$(PKG_FILE)/
	cd $(PKG_DIR) && rm -f $(PKG_FILE).zip && zip -r $(PKG_FILE).zip $(PKG_FILE)/
	@echo
	@echo "Generated $(PKG_DIR)/$(PKG_FILE).zip"

autostart: autorun

autorun: package
	cd $(PKG_DIR)/$(PKG_FILE) && DIME_INSTALL_DIR=$(PWD)/build/libs/ $(PWD)/scripts/package/install-autorun.sh
