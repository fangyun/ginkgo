@echo off
SETLOCAL
set DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y"
set WORK_DIR=%CD%\

if ["%1"] == ["-d"] (
	java -Dginkgo.root=%WORK_DIR% -ea %DEBUG_OPTS% -jar target\ginkgo-0.0.1-SNAPSHOT.jar log-file=%WORK_DIR%log
) else (
	java -Dginkgo.root=%WORK_DIR% -ea -jar target\ginkgo-0.0.1-SNAPSHOT.jar log-file=%WORK_DIR%log
)

ENDLOCAL