package samrock.filter.view;

import static samrock.filter.view.Counts.activity;
import static samrock.filter.view.Counts.add;
import static samrock.filter.view.Counts.downloadHtml;
import static samrock.filter.view.Counts.increment;
import static samrock.filter.view.Counts.remove;
import static samrock.filter.view.Counts.thumbDownloaded;
import static samrock.filter.view.Utils.fx;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.scene.image.Image;
import sam.collection.ArraysUtils;
import sam.console.ANSI;
import sam.internetutils.InternetUtils;
import sam.manga.samrock.thumb.ThumbUtils;
import sam.manga.scrapper.FailedChapter;
import sam.manga.scrapper.ScrappedChapter;
import sam.manga.scrapper.ScrappedManga;
import sam.manga.scrapper.factory.ScrapperFactory;
import sam.myutils.Checker;
import sam.myutils.MyUtilsException;
import sam.myutils.System2;
final class UnitUtils {
	private UnitUtils() { }

	private static final Path thumbsCache = MyUtilsException.noError(() -> {
		Path p = Paths.get(System2.lookup("THUMB_CACHE"));
		Files.createDirectories(p);
		return p;
	});
	private static final ScrapperFactory scrapper = ScrapperFactory.getInstance();
	private static final ExecutorService html = Executors.newSingleThreadExecutor();
	private static final ExecutorService img = Executors.newFixedThreadPool(2);

	public static void load(Unit u) {
		Utils.execute(() -> {
			fx(() -> u.setStatus("loading"));
			downloadHtml(u, !setImg(findThumb(u.manga.id), u));
		});
	}
	private static boolean setImg(Path p, Unit u) {
		Image image = null;

		if(Checker.exists(p)) {
			try(InputStream is = Files.newInputStream(p)) {
				image = new Image(is, Unit.MAX_WIDTH, -1, true, true);
			} catch (IOException e) {
				System.out.println("failed to load: \""+p+"\", error: "+MyUtilsException.toString(e));
				addError(u, e, "image loading error: ", e);
			}
		}
		if(image != null) {
			Image m = image;
			fx(() -> u.setImage(m));
			return true;
		}
		return false;
	}
	private static void downloadHtml(Unit u, boolean loadThumb) {
		Manga m = u.manga;
		html.execute(() -> {
			String url = null;
			fx(() -> u.setStatus("loading html"));
			String sts = "download html: "+toString(m); 
			try {
				add(activity, sts);
				ScrappedManga manga = scrapper.scrap(m.url);
				url = manga.getThumb();
				ScrappedChapter[] chaps = ArraysUtils.removeIf(manga.getChapters(), c -> {
					if(c instanceof FailedChapter) {
						System.out.println(ANSI.red("failed chapter: ")+c);
						return true;
					}
					return false;
				});

				fx(() -> {
					m.setScrappedManga(manga);
					m.setChapters(chaps);
					u.setHtmlLoaded();
				});
				increment(downloadHtml);
			} catch (Exception e) {
				addError(u, e, "failed to extract manga", m);
			} finally {
				remove(activity, sts);
			}
			if(loadThumb) {
				if(url == null)
					addError(u, null, "no thumb url found: ", m);
				else {
					String url2 = url;
					img.execute(() -> downloadThumb(u, url2));
				}
			}
		});
	}

	private static void downloadThumb(Unit u, String url) {
		String s = "download thumb: "+toString(u.manga);
		try {
			add(activity,s);
			Path p = downloadThumb(url, u.manga.id);
			if(setImg(p, u)) 
				increment(thumbDownloaded);
		} catch (IOException e) {
			addError(u, e, "failed to download thumb", toString(u.manga), url);
		} finally {
			remove(activity, s);
		}
	}
	private static void addError(Unit u, Throwable t, Object...msgs) {
		fx(() -> u.addError(t, msgs));
	}
	private static String toString(Manga manga) {
		return manga.id+" | "+manga.mangaName;
	} 

	private static Path findThumb(int mangaId) {
		Path p = ThumbUtils.findThumb(mangaId);
		if(Checker.exists(p))
			return p;

		p = thumbsCache.resolve(String.valueOf(mangaId));
		return Files.exists(p) ? p : null;
	}

	private static final InternetUtils internetUtils = new InternetUtils();

	private static Path downloadThumb(String url, int id) throws MalformedURLException, IOException {
		Path p = thumbsCache.resolve(String.valueOf(id));
		Files.move(internetUtils.download(new URL(url)), p, StandardCopyOption.REPLACE_EXISTING);
		return p;
	}
	public static void shutDown() {
		html.shutdownNow();
		img.shutdownNow();
		
		MyUtilsException.hideError(() -> html.awaitTermination(3, TimeUnit.HOURS), Throwable::printStackTrace);
		MyUtilsException.hideError(() -> img.awaitTermination(3, TimeUnit.HOURS), Throwable::printStackTrace);
	}
}
