@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if "%CATALINA_HOME%"=="" set "CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 9.0"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_503"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"
set "SERVLET_API=%CATALINA_HOME%\lib\servlet-api.jar"
if not exist "%JAVAC%" (echo Java 8 javac not found: %JAVAC% & exit /b 1)
if not exist "%SERVLET_API%" (echo Tomcat servlet-api.jar not found: %SERVLET_API% & exit /b 1)
if not exist "WEB-INF\lib\ojdbc6.jar" (echo Oracle 11g JDBC driver missing from WEB-INF\lib & exit /b 1)
if not exist "WEB-INF\classes" mkdir "WEB-INF\classes"
if exist "HotelBilling.war" del "HotelBilling.war"
if exist build-sources.txt del build-sources.txt
for /r src %%F in (*.java) do (set "SOURCE=%%F" & set "SOURCE=!SOURCE:\=/!" & echo "!SOURCE!" >> build-sources.txt)
"%JAVAC%" -encoding UTF-8 -source 1.8 -target 1.8 -cp "%SERVLET_API%;WEB-INF\lib\ojdbc6.jar" -d WEB-INF\classes @build-sources.txt
if errorlevel 1 exit /b 1
del build-sources.txt
"%JAR%" cf HotelBilling.war -C . WEB-INF -C . styles.css -C . settings.css -C . app-java.css -C . food.css
if errorlevel 1 exit /b 1
echo Built %CD%\HotelBilling.war
echo Copy the WAR to Tomcat's webapps directory after configuring Oracle database access.
endlocal
