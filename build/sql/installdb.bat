@Echo off
cls
echo Вас приветствует скрипт установки сервера Lucera.
echo Этот скрипт вам поможет установить базу данных сервера.
echo Для продолжения нажмите пробел, для завершения Ctrl+C
pause > nul
echo ======================================================================
echo Выполняется проверка окружения...
mysql --help >nul 2>nul
if errorlevel 1 goto nomysql
echo  - MySQL...       ok
echo ======================================================================
echo Сервер Lucera готов к установке. 
echo Пожалуйста, произведите начальную конфигурацию
echo ======================================================================
set DO_INSTALL=Y
set /P DO_INSTALL=Установить логин-сервер[Y/n]
if "%DO_INSTALL%"=="N" goto installgame
if "%DO_INSTALL%"=="n" goto installgame
set INSTALL_MODE=login
:prepare
set DB_HOST=localhost
set DB_USER=root
set DB_PASSWORD=
set DB_NAME=lucera
:step2

set /P DB_HOST=Сервер БД [%DB_HOST%]:

set /P DB_USER=Пользователь БД [%DB_USER%]:

set /P DB_PASSWORD=Пароль пользователя %DB_USER%:

set /P DB_NAME=Имя БД [%DB_NAME%]:
SET MYSQL_PARAM=-u %DB_USER% -h %DB_HOST%
if NOT "%DB_PASSWORD%"=="" SET MYSQL_PARAM=%MYSQL_PARAM% --password=%DB_PASSWORD%
echo exit | mysql %MYSQL_PARAM% >nul 2>nul
if errorlevel 1 goto dberror
echo exit | mysql %MYSQL_PARAM% %DB_NAME% >nul 2>nul
if errorlevel 1 goto dbnotexists
goto install
:dbnotexists
echo  ! База данных %DB_NAME% не существует
set ANSWER=Y
set /P ANSWER=Создать ее [Y/n]?
if "%ANSWER%"=="y" goto createdb
if "%ANSWER%"=="Y" goto createdb
goto step2
:createdb
echo create database %DB_NAME% charset=utf8; | mysql %MYSQL_PARAM%
if errorlevel 1 goto dberror
goto install
:dberror
echo  ! Не могу подключится к БД. Проверьте правильность параметров
goto step2

:install
echo ======================================================================
echo Проверьте правильность введенных параметров
echo   - Сервер будет установлен в %INSTALL_DIR%
echo   - Сервер базы данных %DB_HOST%
echo   - Имя базы данных %DB_NAME%
set ANSWER=Y
set /P ANSWER=Все параметры указаны верно [Y/n]?
if "%ANSWER%"=="n" goto step1
if "%ANSWER%"=="N" goto step1
echo - Устанавливаем БД, подождите...
for %%i in (%INSTALL_MODE%\*.sql) do mysql %MYSQL_PARAM% %DB_NAME% < %%i
if "%INSTALL_MODE%"=="login" goto installgame
goto end
:installgame
set DO_INSTALL=Y
set /P DO_INSTALL=Установить гейм-сервер[Y/n]
if "%DO_INSTALL%"=="N" goto end
if "%DO_INSTALL%"=="n" goto end
set INSTALL_MODE=server
goto prepare 
:nomysql
echo  ! Утилита mysql недоступна
echo  Убедитесь, что mysql.exe находится в переменной окружения PATH
echo  или в текущем каталоге со скриптом установки
goto end
:end
echo Установка завершена, спасибо за выбор нашего продукта
del %TMP%\java.ver