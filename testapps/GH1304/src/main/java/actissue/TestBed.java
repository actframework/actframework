package actissue;

import act.db.ebean.EbeanDao;
import act.db.sql.tx.Transactional;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import javax.inject.Inject;

public class TestBed {
    @Inject
    private EbeanDao<Integer, Project> projDao;
    @Inject
    private EbeanDao<Integer, Author> authDao;

    @GetAction("/authors")
    public Iterable<Author> authorCount() {
        return authDao.findAll();
    }

    @Transactional
    @PostAction
    public void saveAuthorAndProject(Author author, Project project) {
        authDao.save(author);
        projDao.save(project);
    }
}
