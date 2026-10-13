package com.prostovasei.simpletasks.domain.model

import java.time.Instant

/** Папка списков. */
data class ProjectGroup(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Список задач. [color] — HEX-цвет, выбранный пользователем (данные, не дизайн-токен). */
data class Project(
    val id: String,
    val groupId: String?,
    val name: String,
    val color: String?,
    val sortOrder: Int,
    val isInbox: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ProjectWithCount(
    val project: Project,
    val openTaskCount: Int,
)

data class GroupWithProjects(
    val group: ProjectGroup,
    val projects: List<ProjectWithCount>,
)

/** Дерево для экрана «Списки» (SPEC.md §4.3): «Входящие», папки со списками и списки без папки. */
data class ProjectTree(
    val inbox: ProjectWithCount?,
    val groups: List<GroupWithProjects>,
    val ungrouped: List<ProjectWithCount>,
)
