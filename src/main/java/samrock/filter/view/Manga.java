package samrock.filter.view;

import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sam.manga.samrock.urls.MangaUrlsMeta;
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
    public Manga(Row row) {
    	this.id = row.getInt(MANGA_ID);
    	this.mangaName = row.get(MANGA_NAME);
    	this.url = row.get(MangaUrlsMeta.MANGAFOX); 
        this.row0 = row;
    }

    @Override
    public String toString() {
        return row0.toString();
    }
    
    private List<Chapter> nnew, existing, chapters;
    
    public List<Chapter> getNew() {
		return nnew;
	}
    public List<Chapter> getExisting() {
		return existing;
	}
    public List<Chapter> getAll() {
		return chapters;
	}
    public void addChapter(Chapter c) {
    	if(chapters == null)
    		chapters = new ArrayList<>();
    	
    	chapters.add(c);
    	
        if(filter == null)
            nnew = chapters;
        else {
        	if(nnew == null) {
        		nnew = new ArrayList<>();
                existing = new ArrayList<>();
        	}
        	
        	if(filter.test(c.getNumber()))
                existing.add(c);
            else
                nnew.add(c);
        }
    }
    public void scrappingComplete() {
    	chapters = list(chapters);
    	nnew = list(nnew);
    	existing = list(existing);
    }
    private List<Chapter> list(List<Chapter> list) {
		return Checker.isEmpty(list) ? Collections.emptyList() : Collections.unmodifiableList(list);
	}
	public String getDirName() {
        return dir_name;
    }
	public int chaptersCount() {
		return chapters == null ? 0 : chapters.size() ;
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
