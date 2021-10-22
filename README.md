# CloneCoil
[Coil](https://github.com/coil-kt/coil) 라이브러리 따라 만들어보기

## 초기 버전 따라가기
[최초 commit 된 coil](https://github.com/coil-kt/coil/tree/9754d98aae9bb4d4b1a3c15012905b172310608d)
<br>

첫 커밋에 프로젝트가 통째로 되어있어서 어떻게 먼저 구현해야할지 고민
<br>

### 작업 순서
1. kotlin DSL 마이그레이션 및 구현
2. 패키지 구조 정의
3. 인터페이스 추가
4. 인터페이스 구현

## Plugins

### [dokka](https://github.com/Kotlin/dokka)
> Dokka는 Java용 javadoc과 동일한 기능을 수행하는 Kotlin용 문서 엔진입니다. \
  Kotlin 자체와 마찬가지로 Dokka는 혼합 언어 Java/Kotlin 프로젝트를 완벽하게 지원합니다. \
  Java 파일의 표준 Javadoc 주석과 Kotlin 파일의 KDoc 주석을 이해하고 표준 Javadoc, HTML 및 Markdown을 포함한 여러 형식의 문서를 생성할 수 있습니다.

### [gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
> 모든 Java, Kotlin 또는 Android 라이브러리를 모든 Maven 인스턴스에 자동으로 업로드하는 게시 작업을 생성하는 Gradle 플러그인입니다. \
  이 플러그인은 Chris Banes 초기 구현을 기반으로 하며 Kotlin 지원을 추가하고 최신 변경 사항을 따라갈 수 있도록 향상되었습니다.

### [Ktlint Gradle](https://github.com/JLLeitschuh/ktlint-gradle)
> ktlint 프로젝트를 통해 편리한 래퍼 플러그인을 제공합니다.\
  이 플러그인은 Gradle 프로젝트에서 ktlint 검사를 실행하거나 코드 자동 형식을 수행하는 편리한 작업을 만듭니다. \
  플러그인은 모든 프로젝트에 적용할 수 있지만 해당 프로젝트에 kotlin 플러그인이 적용된 경우에만 활성화됩니다. \
  컴파일하지 않은 코드를 린트하고 싶지 않다는 가정입니다.



# Documentation

## Fetcher
> 이미지를 가져올때 사용

### BitmapFetcher
> Bitmap을 받아서 BitmapDrawable 을 변환한 DrawableResult 반환(메모리)

### ResourceFetcher
> @DrawableRes 를 사용해서 resId를 기준으로 DrawableResult(Vector Image)/SourceResult(resource 를 buffered source 로 변환) 2가지 결과를 반환

### DrawableFetcher
> drawable 로 변환한 DrawableResult 반환(메모리)