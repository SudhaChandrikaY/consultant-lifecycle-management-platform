package com.ensar.clmp.common.domain;

/** Projection for grouped count queries: {@code select x.id as groupId, count(x) as count ... group by}. */
public interface GroupCount {

    Long getGroupId();

    long getCount();
}
