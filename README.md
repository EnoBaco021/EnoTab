# 🎮 EnoTab - Minecraft Tab Plugin

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20+-brightgreen?style=for-the-badge&logo=mojang" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java Version">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/github/stars/EnoBaco021/EnoTab?style=for-the-badge" alt="Stars">
</p>

<p align="center">
  <b>Gelişmiş Web Konfigürasyon Panelli Minecraft Tab Plugini</b>
</p>

---

## ✨ Özellikler

- 🌐 **Web Tabanlı Kontrol Paneli** - Port 6969 üzerinden tarayıcı ile kolay konfigürasyon
- 🔐 **Güvenli Admin Girişi** - Session tabanlı oturum yönetimi
- 🎨 **6 Hazır Tab Şablonu** - Tek tıkla profesyonel görünümler
- 🎬 **Animasyonlu Tab** - Header ve footer animasyonları
- 📊 **Gerçek Zamanlı İstatistikler** - TPS, RAM, oyuncu sayısı izleme
- 🔧 **Kolay Placeholder Sistemi** - %player%, %ping%, %tps% ve daha fazlası
- 💾 **Özel Şablon Kaydetme** - Kendi şablonlarınızı oluşturun ve kaydedin
- 📱 **Responsive Tasarım** - Mobil uyumlu modern web paneli

---

## 📦 Kurulum

1. [Releases](https://github.com/EnoBaco021/EnoTab/releases) sayfasından `EnoTab-1.0-SNAPSHOT.jar` dosyasını indirin
2. JAR dosyasını sunucunuzun `plugins` klasörüne koyun
3. Sunucuyu yeniden başlatın
4. Tarayıcınızda `http://sunucu-ip:6969` adresine gidin
5. Varsayılan giriş bilgileri: `admin` / `admin123`

---

## 🎯 Hazır Şablonlar

Web panelinde "Hazır Şablonlar" sekmesinden tek tıkla yükleyebilirsiniz:

| Şablon | Açıklama |
|--------|----------|
| 🎮 **Modern Gaming** | Animasyonlu, modern görünümlü oyun sunucusu teması |
| ⚔️ **PvP Server** | Savaş odaklı, can ve kill göstergeli tema |
| 🏰 **Survival** | Konum ve dünya bilgili survival teması |
| 🌟 **Premium** | VIP görünümlü premium sunucu teması |
| 🎨 **Minimalist** | Sade ve şık minimal tema |
| 🔥 **Hardcore** | Tehlike temalı hardcore sunucu görünümü |

---

## 🖥️ Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/enotab` | Yardım menüsünü gösterir | `enotab.admin` |
| `/enotab reload` | Yapılandırmayı yeniden yükler | `enotab.admin` |
| `/enotab update` | Tüm oyuncuların tab'ını günceller | `enotab.admin` |
| `/enotab web` | Web panel linkini gösterir | `enotab.admin` |
| `/enotab setheader <metin>` | Header metnini değiştirir | `enotab.admin` |
| `/enotab setfooter <metin>` | Footer metnini değiştirir | `enotab.admin` |

**Komut Aliasları:** `/etab`, `/tab`

---

## 🔒 İzinler

| İzin | Açıklama | Varsayılan |
|------|----------|------------|
| `enotab.admin` | Tüm EnoTab komutlarını kullanma | OP |

---

## 📝 Placeholder'lar

### Sunucu
| Placeholder | Açıklama |
|-------------|----------|
| `%online%` | Çevrimiçi oyuncu sayısı |
| `%max%` | Maksimum oyuncu sayısı |
| `%tps%` | Sunucu TPS |
| `%server%` | Sunucu adı |
| `%motd%` | Sunucu MOTD |

### Oyuncu
| Placeholder | Açıklama |
|-------------|----------|
| `%player%` | Oyuncu adı |
| `%displayname%` | Görünen ad |
| `%ping%` | Ping değeri |
| `%health%` | Can |
| `%maxhealth%` | Maksimum can |
| `%food%` | Açlık |
| `%level%` | Seviye |
| `%exp%` | Deneyim yüzdesi |
| `%world%` | Dünya adı |
| `%gamemode%` | Oyun modu |

### Konum & Zaman
| Placeholder | Açıklama |
|-------------|----------|
| `%x%`, `%y%`, `%z%` | Koordinatlar |
| `%time%` | Saat (HH:mm) |
| `%date%` | Tarih (dd/MM/yyyy) |

---

## 🎨 Renk Kodları

```
&0 Siyah        &1 Koyu Mavi    &2 Koyu Yeşil   &3 Koyu Aqua
&4 Koyu Kırmızı &5 Mor          &6 Altın        &7 Gri
&8 Koyu Gri     &9 Mavi         &a Yeşil        &b Aqua
&c Kırmızı      &d Pembe        &e Sarı         &f Beyaz

&l Kalın        &o İtalik       &n Altı Çizili  &m Üstü Çizili
&k Karışık      &r Sıfırla
```

---

## ⚙️ Konfigürasyon Dosyaları

### 📁 config.yml
```yaml
# Web Panel Ayarları
web:
  port: 6969
  enabled: true
  username: admin
  password: admin123
```

### 📁 tab.yml
```yaml
# Header ayarları
header:
  text: "&6&l✦ EnoTab Sunucu ✦\n&7Hoş geldiniz!"
  animated: false
  frames:
    - "&6&l✦ EnoTab Sunucu ✦"
    - "&e&l✦ EnoTab Sunucu ✦"
    - "&f&l✦ EnoTab Sunucu ✦"

# Footer ayarları
footer:
  text: "&7━━━━━━━━━━━━━━━━━━━━━━━\n&eOyuncular: &f%online%&7/&f%max% &8| &eTPS: &f%tps%\n&7━━━━━━━━━━━━━━━━━━━━━━━"
  animated: false
  frames:
    - "&7Ping: &a%ping%ms"
    - "&7TPS: &a%tps%"
    - "&7Oyuncular: &a%online%"

# Oyuncu formatı
player:
  format: "&f%player%"

# Animasyon ayarları
animation:
  interval: 20  # tick cinsinden (20 tick = 1 saniye)

# Güncelleme ayarları
update:
  interval: 40  # tick cinsinden

# Görüntüleme ayarları
display:
  ping: true
  health: false
```

---

## 📂 Dosya Yapısı

```
plugins/EnoTab/
├── config.yml          # Ana konfigürasyon (web panel ayarları)
├── tab.yml             # Tab konfigürasyonu (header, footer, animasyonlar)
└── presets/            # Kayıtlı özel şablonlar
    └── *.json
```

---

## 🛠️ Gereksinimler

| Gereksinim | Versiyon |
|------------|----------|
| Minecraft Server | Paper/Spigot 1.20+ |
| Java | 17 veya üzeri |
| Port | 6969 (Web Panel) |

---

## 🌐 Web Panel Özellikleri

- **Dashboard** - Sunucu istatistikleri (TPS, RAM, oyuncu sayısı)
- **Hazır Şablonlar** - 6 adet hazır tema + özel şablon kaydetme
- **Tab Ayarları** - Header, footer ve oyuncu formatı düzenleme
- **Animasyonlar** - Header/footer animasyon frame'leri yönetimi
- **Oyuncular** - Çevrimiçi oyuncu listesi ve detayları
- **Placeholder'lar** - Kullanılabilir tüm placeholder'ların listesi

---

## 📋 Yapılacaklar

- [ ] PlaceholderAPI desteği
- [ ] Rol/Grup sistemi entegrasyonu
- [ ] Daha fazla hazır şablon
- [ ] Çoklu dil desteği
- [ ] Discord webhook entegrasyonu

---

## 🤝 Katkıda Bulunma

1. Bu repoyu fork edin
2. Yeni bir branch oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/yeni-ozellik`)
5. Pull Request açın

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

---

<p align="center">
  <a href="https://github.com/EnoBaco021">
    <img src="https://img.shields.io/badge/Developer-EnoBaco021-181717?style=for-the-badge&logo=github" alt="Developer">
  </a>
</p>

<p align="center">
  ⭐ Bu projeyi beğendiyseniz yıldız vermeyi unutmayın!
</p>

