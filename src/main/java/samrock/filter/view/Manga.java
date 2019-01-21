package samrock.filter.view;

import static sam.collection.ArraysUtils.removeIf;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_NAME;

import sam.manga.scrapper.ScrappedChapter;
import sam.manga.scrapper.ScrappedManga;
import sam.myutils.Checker;
import sam.tsv.Row;
class Manga {
    final Row row0;
    private ChapterFilter2 filter;
    private String dir_name;
	public final int id;
	public final String mangaName;
	public final String url;
	private ScrappedManga scrapperManga;
	private String status, rank;
    
    public void setSamrock(ChapterFilter2 filter, String dir) {
    	this.filter = filter;
        dir_name = dir;
    }
    public void setMangaRock(String status, String rank) {
    	this.status = status;
        this.rank = rank;
    }
    public Manga(Row row, String urlColumn) {
    	this.id = row.getInt(MANGA_ID);
    	this.mangaName = row.get(MANGA_NAME);
    	this.url = row.get(urlColumn); 
        this.row0 = row;
    }

    @Override
    public String toString() {
        return row0.toString();
    }
    
    private ScrappedChapter[] nnew, existing, chapters;
    private static final ScrappedChapter[] EMPTY = new ScrappedChapter[0];
    
    public ScrappedChapter[] getNew() {
		return nnew;
	}
    public ScrappedChapter[] getExisting() {
		return existing;
	}
    public ScrappedChapter[] getAll() {
		return chapters;
	}
    public void setChapter(ScrappedChapter[] chaps) {
    	this.chapters = chaps;
    	
        if(filter == null)
            nnew = chaps;
        else {
        	existing = removeIf(chaps, c -> !filter.test(c.getNumber()));
        	nnew = removeIf(chaps, c -> filter.test(c.getNumber()));
        }
        
        if(Checker.isEmpty(nnew))
        	nnew = EMPTY;
        if(Checker.isEmpty(existing))
        	existing = EMPTY;
        if(Checker.isEmpty(chapters))
        	chapters = EMPTY;
    }
	public String getDirName() {
        return dir_name;
    }
	public int chaptersCount() {
		return chapters == null ? 0 : chapters.length;
	}
	public void setScrappedManga(ScrappedManga manga) {
		this.scrapperManga = manga;
	}
	public String getThumbUrl() {
		return scrapperManga.getThumb();
	}
	public String getDescription() {
		return scrapperManga == null ? null : scrapperManga.getDescription();
	}
	public ScrappedManga getScrappedManga() {
		return scrapperManga;
	}
	public String getStatus() {
		return status;
	}
	public String getRank() {
		return rank;
	}
	public ChapterFilter2 getFilter() {
		return filter;
	}
}
