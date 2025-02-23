package com.woowacourse.pickgit.portfolio.domain.section;

import com.woowacourse.pickgit.portfolio.domain.Portfolio;
import com.woowacourse.pickgit.portfolio.domain.common.UpdateUtil;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Embeddable
public class Sections {

    @OneToMany(
        mappedBy = "portfolio",
        fetch = FetchType.LAZY,
        cascade = CascadeType.PERSIST,
        orphanRemoval = true
    )
    private final List<Section> values;

    protected Sections() {
        this(new ArrayList<>());
    }

    public Sections(List<Section> values) {
        this.values = values;
    }

    public static Sections empty() {
        return new Sections(new ArrayList<>());
    }

    public void appendTo(Portfolio portfolio) {
        this.values.forEach(section -> section.appendTo(portfolio));
    }

    public void add(Section section) {
        values.add(section);
    }

    public void remove(Section section) {
        values.remove(section);
    }

    public void update(Sections sources, Portfolio portfolio) {
        getValues(sources).forEach(source -> source.appendTo(portfolio));

        UpdateUtil.execute(this.values, sources.values);
    }

    private List<Section> getValues(Sections sources) {
        return sources.values;
    }

    public List<Section> getValues() {
        return values;
    }
}
