git config --global core.autocrlf false
git config --global core.editor vi

# 新增幾個 Hybris 相關環境變數
cat <<EOF > ~/.bash_profile
export HYBRIS_HOME_DIR=/opt/HYBRISCOMM6600P_6-70003031
export ANT_HOME=\${HYBRIS_HOME_DIR}/hybris/bin/platform/apache-ant-1.9.1
export PATH=\${PATH}:\${ANT_HOME}/bin
export INITIAL_ADMIN=nimda
EOF

# 設定幾個好用的 alias 命令
cat <<EOF >> ~/.bash_profile
alias ll='ls --color -lAFh --group-directories-first'
alias ..='cd ..'
alias ...='cd ../../'
alias certinfo='openssl x509 -text -noout -in \$1'

ditto() { directory=\$(dirname \$2); mkdir -p \$directory; cp "\$1" "\$2"; }
EOF

# 調整 Bash 的預設 shopt 值
cat <<EOF >> ~/.bashrc
shopt -s direxpand
shopt -s no_empty_cmd_completion

source ~/.bash_profile
EOF

source ~/.bash_profile

mkdir -p ${HYBRIS_HOME_DIR}/hybris123
cp -a src ${HYBRIS_HOME_DIR}/hybris123/src
cp -a pom.xml ${HYBRIS_HOME_DIR}/hybris123/pom.xml

# 解壓縮 HYBRISCOMM6600P_6-70003031.zip (此為 SAP Commerce 平台程式)
unzip HYBRISCOMM6600P_6-70003031.zip -d ${HYBRIS_HOME_DIR}

echo '>>>> Java 8 Software Development Kit <<<<'
javac -version
echo $JAVA_HOME

echo '>>>> Apache Maven <<<<'
mvn -version

echo '>>>> Git <<<<'
git version

echo '>>>> Chromium <<<<'
chromium -version

echo '>>>> Ant (請使用 SAP Commerce 123 v6.6.0 提供的版本) <<<<'
ant -version

echo '>>>> Gradle (請使用 SAP Commerce 123 v6.6.0 提供的版本) <<<<'
cd ${HYBRIS_HOME_DIR}/installer; ./gradlew -version
