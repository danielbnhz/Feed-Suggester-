
INSERT INTO feeds (title, homepage_url, feed_url, feed_url_hash, language)
SELECT v.title, v.homepage_url, v.feed_url,
       encode(sha256(convert_to(v.feed_url, 'UTF8')), 'hex'),
       'en'
FROM (VALUES
          -- Music production and audio engineering
          ('Attack Magazine',
           'https://www.attackmagazine.com',
           'https://www.attackmagazine.com/feed/'),

          ('Bedroom Producers Blog',
           'https://bedroomproducersblog.com',
           'https://bedroomproducersblog.com/feed/'),

          ('Sound On Sound',
           'https://www.soundonsound.com',
           'https://www.soundonsound.com/latest_news.xml'),

          ('MusicRadar',
           'https://www.musicradar.com',
           'https://www.musicradar.com/feeds.xml'),

          -- Electronic music and synthesis
          ('EDM.com',
           'https://edm.com',
           'https://edm.com/blog/feed/'),

          ('Your EDM',
           'https://www.youredm.com',
           'https://www.youredm.com/feed'),

          ('Mark Mosher',
           'https://markmoshermusic.com',
           'https://markmoshermusic.com/feed/'),

          ('JonDent - Exploring Electronic Music',
           'https://djjondent.blogspot.com',
           'https://djjondent.blogspot.com/feeds/posts/default?alt=rss'),

          -- Independent and experimental music
          ('The Quietus',
           'https://thequietus.com',
           'https://thequietus.com/feed'),

          ('Aquarium Drunkard',
           'https://aquariumdrunkard.com',
           'https://aquariumdrunkard.com/feed/'),

          ('Stereogum',
           'https://www.stereogum.com',
           'https://feeds.feedburner.com/stereogum/cBYa'),

          -- Music journalism and culture
          ('The FADER',
           'https://www.thefader.com',
           'http://www.thefader.com/feed.rss'),

          ('Consequence - Music',
           'https://consequence.net/category/music',
           'https://consequence.net/category/music/feed/'),

          ('NME - Music News',
           'https://www.nme.com/news/music',
           'https://www.nme.com/news/music/feed'),

          ('SPIN',
           'https://www.spin.com',
           'https://www.spinmagazine.com/feed/'),

          -- Music industry and creative technology
          ('Music Business Worldwide',
           'https://www.musicbusinessworldwide.com',
           'https://www.musicbusinessworldwide.com/feed/'),

          ('MuseWire',
           'https://musewire.com',
           'https://musewire.com/feed/')

     ) AS v(title, homepage_url, feed_url)
    ON CONFLICT (feed_url_hash) DO NOTHING;
