Bu proje CMS yapısına hizmet edecektir. Elly Güçlü Bir İçerik Yönetim Sistemi Olarak Hızlı Bir Website Yapılandırmasını sağlayacak

#Entity
Page
Seo Info
Component
Banner
Widget
Post
Article
Comment
Rating


#Yapının Çalışma Şekli
Page: Sayfalar oluşturulur seo bilgileri bu kayıt'ta alınır.
Component: Component sayfalara eklenebilir. bir component birden fazla sayfada kullanabilir. componentlerin içine banner ve widget eklenebilir 2 tip ComponentType vardır BANNER ve WIDGET
Banner: ComponentType BANNER ise Component içine eklenebilir. | WidgetType BANNER ise Widget içine eklenebilir.
Widget: ComponentType WIDGET ise Component içine eklenebilir. widgetların içine banner ve post eklenebilir 3 tip WidgetType vardır BANNER, POST ve ARTICLE
Post: WidgetType POST ise Widget içine eklenebilir.
Article: WidgetType ARTICLE ise Widget içine eklenebilir.
Comment: Post ve Article için geçerli olacak ve bir üst commente baglanabilir olan commentler olabilir.
Rating: Post ve Article için geçerli olacak

#Sayfalar
Sayfalar oluşturulduğunda Seo bilgileri bu kayıt işleminde alınır.
Sayfalara birden fazla Component atanabilir.

#Component
Componentler birden fazla sayfalara eklenebilir.
ComponentType ile BANNER ve WIDGET olarak ayrılır.
BANNER : ComponentType BANNER ise sadece banner tablosundan veri alarak görselleri getiren bir componente dönüşür
WIDGET : ComponentType WIDGET ise sadece widget tablosundan veri alarak widget'a göre değişen bir yapı getirir.

#Banner
birden fazla banner Component ve Widget a ait olabilir.

#Widget
Widgetlar birden fazla Componentlere eklenebilr.
WidgetType ile BANNER,POST ve ARTICLE olarak ayrılır.
BANNER : WidgetType BANNER ise sadece banner tablosundan veri alarak görselleri getiren bir widgeta dönüşür
POST : WidgetType POST ise sadece post tablosundan veri alarak post yapısını getiren bir widgeta dönüşür
ARTICLE : WidgetType ARTICLE ise sadece article tablosundan veri alarak article yapısını getiren bir widgeta dönüşür

#Post
birden fazla post Widget a ait olabilir.

#Article
birden fazla article Widget a ait olabilir.

#Comment
birden fazla comment Post ve Article a ait olabilir.

#Rating
birden fazla rating Post ve Article a ait olabilir.