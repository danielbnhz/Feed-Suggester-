INSERT INTO feeds (title, homepage_url, feed_url, feed_url_hash, language)
SELECT v.title, v.homepage_url, v.feed_url,
       encode(sha256(convert_to(v.feed_url, 'UTF8')), 'hex'),
       'en'
FROM (VALUES
          ('Hacker News',          'https://news.ycombinator.com',        'https://news.ycombinator.com/rss'),
          ('Lobsters',             'https://lobste.rs',                   'https://lobste.rs/rss'),
          ('Ars Technica',         'https://arstechnica.com',             'https://feeds.arstechnica.com/arstechnica/index'),
          ('The Verge',            'https://www.theverge.com',            'https://www.theverge.com/rss/index.xml'),
          ('Wired',                'https://www.wired.com',               'https://www.wired.com/feed/rss'),
          ('Daring Fireball',      'https://daringfireball.net',          'https://daringfireball.net/feeds/main'),
          ('Kotlin Blog',          'https://blog.jetbrains.com/kotlin',   'https://blog.jetbrains.com/kotlin/feed/'),
          ('CSS-Tricks',           'https://css-tricks.com',              'https://css-tricks.com/feed/'),
          ('Smashing Magazine',    'https://www.smashingmagazine.com',    'https://www.smashingmagazine.com/feed/'),
          ('Quanta Magazine',      'https://www.quantamagazine.org',      'https://api.quantamagazine.org/feed/'),
          ('Nature',               'https://www.nature.com',              'https://www.nature.com/nature.rss'),
          ('Phys.org',             'https://phys.org',                    'https://phys.org/rss-feed/'),
          ('Aeon',                 'https://aeon.co',                     'https://aeon.co/feed.rss'),
          ('Create Digital Music', 'https://cdm.link',                    'https://cdm.link/feed/'),
          ('BBC News',             'https://www.bbc.com/news',            'https://feeds.bbci.co.uk/news/rss.xml')
     ) AS v(title, homepage_url, feed_url)
    ON CONFLICT (feed_url_hash) DO NOTHING;