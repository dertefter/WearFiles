<p align="center">  
  <img src="art/banner.png" alt="Banner">
</p>  

<a href="https://play.google.com/store/apps/details?id=com.dertefter.wearfiles">  
  <img src="https://github.com/dertefter/some_stuff_for_me/blob/master/com.dertefter.wearfiles_downloads.svg" alt="Google Play" height="60">  
</a>  
<a href="https://play.google.com/store/apps/details?id=com.dertefter.wearfiles">  
  <img src="https://github.com/dertefter/some_stuff_for_me/blob/master/com.dertefter.wearfiles_rating.svg" alt="Google Play" height="60">  
</a>
<a href="README.md">
  <img src="https://img.shields.io/badge/English-blue?style=for-the-badge" alt="English language" height="60">
</a>

# WearFiles

Простой файловый менеджер с открытым исходным кодом. Разработан для Wear OS ⌚

### Возможности приложения:

- Передача файлов с телефона на часы ⌚➡️📱
- Просмотр, открытие, удаление файлов на устройстве Wear OS 📂
- Буфер обмена: вырезать / копировать / вставить 📋
- Закрепление файлов и папок на главном экране 📌
- Встроенный просмотрщик изображений 🖼️
- Встроенный просмотрщик PDF 📄

### Скриншоты
<p align="center">  
  <img src="art/screenshot_1.png" width="160" alt="Screenshot 1">  
  <img src="art/screenshot_2.png" width="160" alt="Screenshot 2">  
  <img src="art/screenshot_3.png" width="160" alt="Screenshot 3">  
  <img src="art/screenshot_4.png" width="160" alt="Screenshot 4">  
  <img src="art/screenshot_5.png" width="160" alt="Screenshot 5">  
</p>  

### Передача файлов с телефона на часы:

Вы можете передавать файлы со смартфона на часы. Для этого установите приложение на оба устройства и убедитесь в том, что связь с часами установлена.

<p align="center">
  <img src="art/example_file_transfer.gif" width="320" alt="example_file_transfer">
</p>

### ⚠️ Разрешение на доступ к файлам ⚠️

Чтобы использовать приложение на часах как файловый менеджер вам необходимо выдать разрешение на доступ к файлам: `MANAGE_EXTERNAL_STORAGE`. Из-за ограничений платформы Wear OS приложение не может самостоятельно выдать разрешения на доступ к файлам.
Однако вы можете пользоваться приложением и без этого в ограниченном режиме. Вероятно, что **полный доступ к файлам вам не нужен, если вы хотите просто передать файл с телефона и открыть его на часах**.

#### Что работает без разрешения `MANAGE_EXTERNAL_STORAGE` :

- «Фото»: просмотр списка изображений, сохранённых на устройстве
- «Видео»: видео, сохранённые на устройстве
- «Музыка»: список аудиофайлов на устройстве
- «Получено»: файлы, полученные с телефона
- Открытие вышеперечисленных файлов (при налиии ПО для открытия этих типов файлов)

#### Как выдать разрешение вручную:
Если вы решили, что вам необходим полный доступ к файлам, вы можете выдать разрешение с помощью ADB:

1. Подключите часы к компьютеру через ADB
2. Выполните команду:  
   ``adb shell appops set --uid com.dertefter.wearfiles MANAGE_EXTERNAL_STORAGE allow``
3. Перезапустите приложение

### 💎 Поддержать меня:
[https://www.donationalerts.com/r/dertefter](https://www.donationalerts.com/r/dertefter)

### Звёздочки

<a href="https://star-history.dera.page/#dertefter/WearFiles">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://star-history.dera.page/svg?repos=dertefter/WearFiles&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://star-history.dera.page/svg?repos=dertefter/WearFiles" />
   <img alt="Star History Chart" src="https://star-history.dera.page/svg?repos=dertefter/WearFiles" />
 </picture>
</a>