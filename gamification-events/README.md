# Библиотека событий геймификации

Библиотека событий для системы геймификации LMS.

## Описание

Библиотека предоставляет типизированные, иммутабельные DTO для событий геймификации,
использующие современные возможности Java 21 (Records, Sealed Interfaces).

## Структура событий

### Базовые события (GamificationEvent)

- TaskCompletedEvent - завершение задачи
- TestPassedEvent - прохождение теста
- CourseEnrolledEvent - запись на курс
- ForumPostCreatedEvent - создание поста на форуме
- AssignmentSubmittedEvent - сдача задания

### Внутренние события

- PointsChangedEvent - изменение баланса очков

## Подключение к другим модулям

```xml
<dependency>
    <groupId>ru.misis.gamification</groupId>
    <artifactId>gamification-events</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Собрать модуль

```
mvn clean install -pl gamification-events
```

## Javadoc для модуля

### Генерирует javadoc в target/site/apidocs

```
mvn javadoc:javadoc -pl gamification-events
```

### Открыть сгенерированную документацию в браузере:PointsChangedEvent

```
# (Linux/Mac)
open gamification-events/target/site/apidocs/index.html
```

```
# (Windows)
start gamification-events/target/site/apidocs/index.html
```

## Собрать source JAR отдельно

```
mvn source:jar -pl gamification-events
```