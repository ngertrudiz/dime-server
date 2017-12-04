GRADLE   = ./gradlew -q
TARGET   = build/libs/dime-server.jar
JAVADOC_DIR = build/docs/javadoc/
JAVADOC_WEB = shell.hiit.fi:/group/reknow/public_html/javadoc/dime-server/
APIDOC_BIN = ./node_modules/.bin/apidoc
APIDOC_DIR = build/docs/apidoc/
APIDOC_WEB = shell.hiit.fi:/group/reknow/public_html/apidoc/dime-server/
PKG_DIR  = build/package
PKG_FILE = dime
DIME_PORT = $(shell test -f config/application-local.properties && ((grep '^server.port=' config/application-local.properties || echo server.port=8080) | sed 's/.*port=//') || echo 8080)
SOURCES := $(shell find src/ -name '[A-Z]*.java' -or -name '*.html' -or -name 'db*.xml' -or -name '*.properties') build.gradle

DIMEUI_SUBMODULE = submodules/dime-ui/package.json
DIMEUI_OUTDIR = src/main/resources/static
DIMEUI_TARGET = $(DIMEUI_OUTDIR)/index.html
DIMEUI_SOURCE = $(addprefix submodules/dime-ui/,$(shell git -C submodules/dime-ui ls-files))

DIME_HOME = ~/.dime

DOCKER_DIR = $(DIME_HOME)/docker
DB_FILE = $(DIME_HOME)/database/h2.mv.db
TMP_DIR = $(DIME_HOME)/tmp

LOG_HEAD = "[MAKE]"

all:	assemble

assemble:  $(TARGET)

$(TARGET): $(SOURCES) $(DIMEUI_TARGET)
	$(GRADLE) assemble

$(DIMEUI_TARGET): $(DIMEUI_SUBMODULE) $(DIMEUI_SOURCE)
	cd submodules/dime-ui; npm install; npm run build
	rsync -var submodules/dime-ui/build/ src/main/resources/static/

dimeui: $(DIMEUI_TARGET)

$(DIMEUI_SUBMODULE):
	git submodule init
	git submodule update

dimeui-clean:
	rm -f $(DIMEUI_OUTDIR)/index.html
	rm -f $(DIMEUI_OUTDIR)/*.chunk.js
	rm -f $(DIMEUI_OUTDIR)/main.*.js
	rm -f $(DIMEUI_OUTDIR)/main.*.css
	rm -f $(DIMEUI_OUTDIR)/manifest.json
	rm -f $(DIMEUI_OUTDIR)/vendor.*.js

run:    $(TARGET)
	@lsof -i :$(DIME_PORT) -sTCP:LISTEN && echo '\nERROR: DiMe already running in port $(DIME_PORT)!' || java -jar $(TARGET)

test:
	rm -rf ./build/reports/tests/
	rm -rf $(TMP_DIR)
	SPRING_PROFILES_ACTIVE=cleandb $(GRADLE) test
	@echo $(LOG_HEAD) Now open ./build/reports/tests/index.html

testone:
	rm -rf ./build/reports/tests/
	rm -rf $(TMP_DIR)
	SPRING_PROFILES_ACTIVE=cleandb $(GRADLE) test --tests fi.hiit.dime.ApiControllerTest.testUserDeletionBug*

clean: dimeui-clean
	$(GRADLE) clean

doc: $(SOURCES)
	$(GRADLE) javadoc
	chmod -R a+r $(JAVADOC_DIR)
	rsync -var $(JAVADOC_DIR) $(JAVADOC_WEB)
	@echo
	@echo $(LOG_HEAD) Now open $(JAVADOC_DIR)/index.html

apidoc: $(SOURCES) $(APIDOC_BIN)
	$(APIDOC_BIN) -i src/main/java -o $(APIDOC_DIR)
	chmod -R a+r $(APIDOC_DIR)
	rsync -var $(APIDOC_DIR) $(APIDOC_WEB)
	@echo
	@echo $(LOG_HEAD) Now open $(APIDOC_DIR)/index.html

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
	rm -rf $(TMP_DIR)
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
