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

### UriFetcher
> Uri 로 SourceResult(BufferedSource) 를 반환(디스크)

### HttpUrlFetcher
> HttpUrl 로 SourceResult 를 반환, DataSource.DISK or DataSource.NETWORK


## Decoder
> BufferedSource를 Drawable로 변환합니다.

### EmptyDecoder
> 소스를 소진하고 하드코딩된 빈 결과를 반환하는 Decoder 입니다.

### BitmapFactoryDecoder
> BitmapFactory를 사용하여 지정된 BufferedSource 디코딩을 시도하는 기본 Decoder입니다.


## BitmapPoolStrategy
> BitmapPool 전략, OS Level 에 따라 다른 전략을 채택

### SizeStrategy
> Bitmap.reconfigure에 의존하는 비트맵 재사용 전략.

- M 이상에서 사용
- GroupedLinkedMap 의 키를 Bitmap의 메모리 크기의 바이트 단위를 사용
- LRU Pool 을 사용

### SizeConfigStrategy
> Bitmap.getAllocationByteCountCompat 및 Bitmap.Config를 모두 사용하는 Bitmap 키입니다.\
  Bitmap의 할당 횟수와 구성을 모두 사용하면 더 다양한 Bitmap을 안전하게 재사용할 수 있으므로 풀의 적중률이 증가하여 애플리케이션의 성능이 향상됩니다.

- KITKAT 이상에서 사용

### AttributeStrategy
> 반환된 비트맵의 치수가 요청한 치수와 정확히 일치해야 하는 비트맵 재사용 전략입니다.

- JELLY_BEAN(18) 이하 버전에서 사용
- GroupedLinkedMap 의 키를 width, height, Bitmap.Config 3가지를 사용
- width, height, Bitmap.Config 3가지가 다르면 다른 비트맵으로 인지


## Transformation
> 이미지의 픽셀 데이터를 변환하기 위한 인터페이스입니다.

### GrayscaleTransformation
> 이미지를 회색 음영으로 변환하는 Transformation입니다.

### RoundedCornersTransformation
> 이미지의 모서리를 둥글게 만드는 Transformation

### CircleCropTransformation
> 가운데 원을 마스크로 사용하여 이미지를 자르는 Transformation

### BlurTransformation
> 이미지에 가우시안 블러를 적용하는 Transformation

### 참고
[이미지 합성, PorterDuff.Mode](https://developer.android.com/reference/android/graphics/PorterDuff.Mode)


## Memory

### BitmapReferenceCounter
> Bitmap에 대한 참조를 계산합니다. 더 이상 참조되지 않는 경우 bitmapPool에 비트맵을 추가합니다.

### MemoryCache
> 최근에 메모리에 로드된 Bitmap에 대한 LRU 캐시입니다.

### RequestService
> Request에 대해 작동하는 작업을 처리합니다.

### DelegateService
> DelegateService는 Target을 래핑하여 Bitmap 풀링을 지원하고 Request를 래핑하여 수명 주기를 관리합니다.


## RequestBuilder
> [LoadRequestBuilder] 및 [GetRequestBuilder]의 기본 클래스입니다.

### LoadRequestBuilder
> [LoadRequest]를 위한 빌더.

### GetRequestBuilder
> [GetRequest]를 위한 빌더.


## Request
> 이미지 요청을 나타내는 값 개체입니다.

### GetRequest
> get 이미지 요청을 나타내는 값 개체입니다.

### LoadRequest
> load 이미지 요청을 나타내는 값 개체입니다.

## ComponentRegistry
> [ImageLoader]가 이미지 요청을 이행하기 위해 사용하는 모든 구성 요소에 대한 레지스트리.\
  이 클래스를 사용하여 사용자 정의 [Mapper], [MeasuredMapper], [Fetcher] 및 [Decoder]에 대한 지원을 등록합니다.