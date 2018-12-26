package samrock.filter.view;

import static sam.manga.mangarock.MangarockMangaMeta.ID;
import static sam.manga.mangarock.MangarockMangaMeta.RANK;
import static sam.manga.mangarock.MangarockMangaMeta.STATUS;
import static sam.manga.mangarock.MangarockMangaMeta.TABLE_NAME;
import static sam.sql.querymaker.QueryMaker.qm;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sam.config.MyConfig;
import sam.fx.alert.FxAlert;
import sam.myutils.System2;
import sam.sql.querymaker.QueryMaker;
import sam.sql.sqlite.SQLiteDB;

public class Mangarock implements AutoCloseable {
	private SQLiteDB db;
	private boolean modified;

	public Mangarock() throws SQLException {
		String p = MyConfig.MANGAROCK_INPUT_DB;
		if(Files.notExists(Paths.get(p)))
			p = MyConfig.MANGAROCK_DB_BACKUP;
		if(Files.notExists(Paths.get(p)))
			p = null;

		if(p == null) {
			System.out.println("not found: mangarock.db");
			FxAlert.showErrorDialog(MyConfig.MANGAROCK_INPUT_DB+"\n"+MyConfig.MANGAROCK_DB_BACKUP, "not found: mangarock.db", null);
			return;
		}

		System.out.println("mangarock.db: "+p);

		db = new SQLiteDB(p);
	}

	public void remove(int[] ids) throws SQLException {
		int executes = db.executeUpdate(QueryMaker.getInstance().deleteFrom("Favorites").where(w -> w.in("manga_id", ids)).build());
		System.out.println("executes: "+executes);
		modified = true;
	}

	@Override
	public void close() throws Exception {
		if(modified && FxAlert.showConfirmDialog(null, "Save Changes to Mangarock"))
			db.commit();
		db.close();
	}

	public void loadMangaRock(List<Manga> mangas) throws SQLException {
		if(!System2.lookupBoolean("MANGAROCK_LOAD", true)) {
			mangas.forEach(m -> m.setMangaRock("-1", "-1"));
			return;
		}
		
		Map<Integer, Manga> map = mangas.stream().collect(Collectors.toMap(m -> m.id, m -> m));
		
		String sql = qm().select(ID, STATUS, RANK)
				.from(TABLE_NAME)
				.where(w -> w.in(ID, map.keySet()))
				.build();

		db.iterate(sql, rs -> {
			int id = rs.getInt(ID);
			String status = rs.getBoolean(STATUS) ? "COMPLETED" : "ONGOING";
			String rank = rs.getString(RANK);
			map.get(id).setMangaRock(status, rank);
		});
	}
}
