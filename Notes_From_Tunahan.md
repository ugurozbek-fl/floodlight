* AlgorithmExecutor() içerisindeki runAlgorithms() metodu sadece belirli düğümler arasındaki yol hesaplamaları için kullanılmaktadır.
  O sebeple sadece belirli düğümler arasında çalıştırılmaktadır.
  
* linkBandWith her bir hesaplama anında üretilen random değerleri tutan veri yapısıdır. Çalışma esnasında bağlantı cost değerlerine katkı amaçlı kullanılmıştır.
* initLinkBandWith metodu yazılmıştır buna bağlı olarak

* initLinkCostMap override edildi

* NodeDist inner class iken normal class haline getirdim.(Disjktra metodunu başka class'a taşıdım)
