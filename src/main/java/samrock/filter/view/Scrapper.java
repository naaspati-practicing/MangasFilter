package samrock.filter.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import sam.internetutils.InternetUtils;
import sam.manga.samrock.thumb.ThumbUtils;
import sam.manga.scrapper.ScrappedManga;
import sam.manga.scrapper.scrappers.impl.ScrapperCached;

public class Scrapper  {
    private static volatile Scrapper instance;

    public static Scrapper getInstance() {
        if (instance == null) {
            synchronized (Scrapper.class) {
                if (instance == null) {
                    try {
                        instance = new Scrapper();
                    } catch (IOException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return instance;
    }

    private final Path thumbsCache;
    private final ScrapperCached scrapper;

    public Path findThumb(int mangaId) {
        Path p = ThumbUtils.findThumb(mangaId);
        if(p != null)
            return p;
        
        p = thumbsCache.resolve(String.valueOf(mangaId));
        return Files.exists(p) ? p : null;
    }
    
    private InternetUtils internetUtils;
    
    public Path downloadThumb(Manga manga) throws MalformedURLException, IOException {
    	Path p = thumbsCache.resolve(String.valueOf(manga.id));
    	
    	if(internetUtils == null)
    		internetUtils = new InternetUtils();
    	
    	Files.move(internetUtils.download(new URL(manga.getThumbUrl())), p, StandardCopyOption.REPLACE_EXISTING);
    	return p;
    }
    protected Scrapper() throws IOException, InstantiationException, IllegalAccessException {
    	scrapper = ScrapperCached.createDefaultInstance();
    	thumbsCache = scrapper.cacheDir.toPath().resolve("THUMBS");
    	Files.createDirectories(thumbsCache);
    }
	public ScrappedManga scrapManga(String url) throws Exception {
		return scrapper.scrapManga(url);
	}
}
