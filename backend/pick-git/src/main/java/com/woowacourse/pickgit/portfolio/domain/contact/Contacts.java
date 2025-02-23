package com.woowacourse.pickgit.portfolio.domain.contact;

import com.woowacourse.pickgit.portfolio.domain.Portfolio;
import com.woowacourse.pickgit.portfolio.domain.common.UpdateUtil;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Embeddable
public class Contacts {

    @OneToMany(
        mappedBy = "portfolio",
        fetch = FetchType.LAZY,
        cascade = CascadeType.PERSIST,
        orphanRemoval = true
    )
    private final List<Contact> values;

    protected Contacts() {
        this(new ArrayList<>());
    }

    public Contacts(List<Contact> values) {
        this.values = values;
    }

    public static Contacts empty() {
        return new Contacts(new ArrayList<>());
    }

    public void appendTo(Portfolio portfolio) {
        values.forEach(contact -> contact.appendTo(portfolio));
    }

    public void update(Contacts sources, Portfolio portfolio) {
        getValues(sources).forEach(contact -> contact.appendTo(portfolio));

        UpdateUtil.execute(this.values, sources.values);
    }

    private List<Contact> getValues(Contacts sources) {
        return sources.values;
    }

    public List<Contact> getValues() {
        return values;
    }
}
