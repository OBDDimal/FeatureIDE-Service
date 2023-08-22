package de.featureide.service.data

import de.featureide.service.data.dao.FeatureModelStatFileDAO
import de.featureide.service.models.FeatureModelStatFile
import de.featureide.service.models.FeatureModelStatFiles
import de.ovgu.featureide.fm.core.Features
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class FeatureModelStatFileDataSource : FeatureModelStatFileDAO {

    private fun resultRowToFile(row: ResultRow) = FeatureModelStatFile(
        id = row[FeatureModelStatFiles.id],
        name = row[FeatureModelStatFiles.name],
        content = row[FeatureModelStatFiles.content],
        deadFeatures = row[FeatureModelStatFiles.deadFeatures].split(":::").toTypedArray(),
        falseOptionalFeatures = row[FeatureModelStatFiles.falseOptionalFeatures].split(":::").toTypedArray(),
        coreFeatures = row[FeatureModelStatFiles.coreFeatures].split(":::").toTypedArray()
    )

    override suspend fun getFile(id: Int): FeatureModelStatFile? = DatabaseFactory.dbQuery {
        FeatureModelStatFiles.select(FeatureModelStatFiles.id eq id).map(::resultRowToFile).singleOrNull()
    }

    override suspend fun addFile(): FeatureModelStatFile? = DatabaseFactory.dbQuery {
        val insert = FeatureModelStatFiles.insert {
            it[name] = ""
            it[content] = ""
            it[deadFeatures] = ""
            it[falseOptionalFeatures] = ""
            it[coreFeatures] = ""
        }

        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun isReady(id: Int): Boolean = DatabaseFactory.dbQuery {
        FeatureModelStatFiles.select(FeatureModelStatFiles.id eq id).map(::resultRowToFile).singleOrNull()?.content.isNullOrBlank()
    }


    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        FeatureModelStatFiles.deleteWhere { FeatureModelStatFiles.id eq id } > 0
    }

    override suspend fun update(id: Int, content: String, name: String, deadFeatures: Array<String>, falseOptionalFeatures: Array<String>, coreFeatures: Array<String>): Boolean =
        DatabaseFactory.dbQuery {
            FeatureModelStatFiles.update({ FeatureModelStatFiles.id eq id }) {
                it[FeatureModelStatFiles.name] = name
                it[FeatureModelStatFiles.content] = content
                it[FeatureModelStatFiles.deadFeatures] = deadFeatures.joinToString(":::")
                it[FeatureModelStatFiles.falseOptionalFeatures] = falseOptionalFeatures.joinToString(":::")
                it[FeatureModelStatFiles.coreFeatures] = coreFeatures.joinToString(":::")
            } > 0
        }


}

val featureModelStatFileDataSource: FeatureModelStatFileDAO = FeatureModelStatFileDataSource()