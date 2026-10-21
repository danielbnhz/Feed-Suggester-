INSERT INTO feeds (title, homepage_url, feed_url, feed_url_hash, language)
SELECT v.title, v.homepage_url, v.feed_url,
       encode(sha256(convert_to(v.feed_url, 'UTF8')), 'hex'),
       'en'
FROM (VALUES
          -- Tech and software
          ('TechCrunch',            'https://techcrunch.com',                 'https://techcrunch.com/feed/'),
          ('Engadget',              'https://www.engadget.com',               'https://www.engadget.com/rss.xml'),
          ('Gizmodo',               'https://gizmodo.com',                    'https://gizmodo.com/rss'),
          ('MIT Technology Review', 'https://www.technologyreview.com',       'https://www.technologyreview.com/feed/'),
          ('IEEE Spectrum',         'https://spectrum.ieee.org',              'https://spectrum.ieee.org/feeds/feed.rss'),
          ('Hackaday',              'https://hackaday.com',                   'https://hackaday.com/blog/feed/'),
          ('GitHub Blog',           'https://github.blog',                    'https://github.blog/feed/'),
          ('Stack Overflow Blog',   'https://stackoverflow.blog',             'https://stackoverflow.blog/feed/'),
          ('freeCodeCamp',          'https://www.freecodecamp.org/news',      'https://www.freecodecamp.org/news/rss/'),
          ('Real Python',           'https://realpython.com',                 'https://realpython.com/atom.xml'),

          -- Web and design
          ('A List Apart',          'https://alistapart.com',                 'https://alistapart.com/main/feed/'),
          ('Codrops',               'https://tympanus.net/codrops',           'https://tympanus.net/codrops/feed/'),

          -- Science and space
          ('Scientific American',   'https://www.scientificamerican.com',     'https://rss.sciam.com/ScientificAmerican-Global'),
          ('New Scientist',         'https://www.newscientist.com',           'https://www.newscientist.com/feed/home/'),
          ('ScienceDaily',          'https://www.sciencedaily.com',           'https://www.sciencedaily.com/rss/all.xml'),
          ('NASA',                  'https://www.nasa.gov',                   'https://www.nasa.gov/feed/'),
          ('Space.com',             'https://www.space.com',                  'https://www.space.com/feeds/all'),
          ('Live Science',          'https://www.livescience.com',            'https://www.livescience.com/feeds/all'),
          ('Smithsonian Magazine',  'https://www.smithsonianmag.com',         'https://www.smithsonianmag.com/rss/latest_articles/'),

          -- Ideas and culture
          ('Psyche',                'https://psyche.co',                      'https://psyche.co/feed'),
          ('Big Think',             'https://bigthink.com',                   'https://bigthink.com/feed/'),
          ('The Marginalian',       'https://www.themarginalian.org',         'https://www.themarginalian.org/feed/'),
          ('Noema Magazine',        'https://www.noemamag.com',               'https://www.noemamag.com/feed/'),
          ('Literary Hub',          'https://lithub.com',                     'https://lithub.com/feed/'),

          -- News
          ('The Guardian (World)',  'https://www.theguardian.com/world',      'https://www.theguardian.com/world/rss'),
          ('Al Jazeera',            'https://www.aljazeera.com',              'https://www.aljazeera.com/xml/rss/all.xml'),
          ('NPR',                   'https://www.npr.org',                    'https://feeds.npr.org/1001/rss.xml'),
          ('Deutsche Welle',        'https://www.dw.com/en',                  'https://rss.dw.com/rdf/rss-en-all'),

          -- Music
          ('Synthtopia',            'https://www.synthtopia.com',             'https://www.synthtopia.com/feed/'),
          ('Bandcamp Daily',        'https://daily.bandcamp.com',             'https://daily.bandcamp.com/feed'),
          ('Pitchfork',             'https://pitchfork.com',                  'https://pitchfork.com/feed/feed-news/rss'),

          -- Games
          ('PC Gamer',              'https://www.pcgamer.com',                'https://www.pcgamer.com/rss/'),
          ('Rock Paper Shotgun',    'https://www.rockpapershotgun.com',       'https://www.rockpapershotgun.com/feed'),
          ('Eurogamer',             'https://www.eurogamer.net',              'https://www.eurogamer.net/feed'),
          ('Polygon',               'https://www.polygon.com',                'https://www.polygon.com/rss/index.xml')
     ) AS v(title, homepage_url, feed_url)
    ON CONFLICT (feed_url_hash) DO NOTHING;