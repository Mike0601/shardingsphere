/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.context.refresher.type;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.context.refresher.MetaDataRefresher;
import org.apache.shardingsphere.infra.federation.optimizer.context.planner.OptimizerPlannerContext;
import org.apache.shardingsphere.infra.federation.optimizer.context.planner.OptimizerPlannerContextFactory;
import org.apache.shardingsphere.infra.federation.optimizer.metadata.FederationDatabaseMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.builder.GenericSchemaBuilder;
import org.apache.shardingsphere.infra.metadata.database.schema.builder.GenericSchemaBuilderMaterials;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.metadata.database.schema.event.MetaDataRefreshedEvent;
import org.apache.shardingsphere.infra.metadata.database.schema.event.SchemaAlteredEvent;
import org.apache.shardingsphere.infra.rule.identifier.type.DataNodeContainedRule;
import org.apache.shardingsphere.infra.rule.identifier.type.MutableDataNodeRule;
import org.apache.shardingsphere.sql.parser.sql.common.segment.ddl.table.RenameTableDefinitionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.RenameTableStatement;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Schema refresher for rename table statement.
 */
public final class RenameTableStatementSchemaRefresher implements MetaDataRefresher<RenameTableStatement> {
    
    @Override
    public Optional<MetaDataRefreshedEvent> refresh(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData,
                                                    final Map<String, OptimizerPlannerContext> optimizerPlanners,
                                                    final Collection<String> logicDataSourceNames, final String schemaName, final RenameTableStatement sqlStatement,
                                                    final ConfigurationProperties props) throws SQLException {
        SchemaAlteredEvent event = new SchemaAlteredEvent(database.getName(), schemaName);
        for (RenameTableDefinitionSegment each : sqlStatement.getRenameTables()) {
            String tableName = each.getTable().getTableName().getIdentifier().getValue();
            String renameTable = each.getRenameTable().getTableName().getIdentifier().getValue();
            putTableMetaData(database, federationDatabaseMetaData, optimizerPlanners, logicDataSourceNames, schemaName, renameTable, props);
            removeTableMetaData(database, federationDatabaseMetaData, optimizerPlanners, schemaName, tableName);
            event.getAlteredTables().add(database.getSchema(schemaName).get(renameTable));
            event.getDroppedTables().add(tableName);
        }
        return Optional.of(event);
    }
    
    private void removeTableMetaData(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData,
                                     final Map<String, OptimizerPlannerContext> optimizerPlanners, final String schemaName, final String tableName) {
        database.getSchema(schemaName).remove(tableName);
        database.getRuleMetaData().findRules(MutableDataNodeRule.class).forEach(each -> each.remove(schemaName, tableName));
        federationDatabaseMetaData.removeTableMetadata(schemaName, tableName);
        optimizerPlanners.put(federationDatabaseMetaData.getName(), OptimizerPlannerContextFactory.create(federationDatabaseMetaData));
    }
    
    private void putTableMetaData(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData, final Map<String, OptimizerPlannerContext> optimizerPlanners,
                                  final Collection<String> logicDataSourceNames, final String schemaName, final String tableName, final ConfigurationProperties props) throws SQLException {
        if (!containsInImmutableDataNodeContainedRule(tableName, database)) {
            database.getRuleMetaData().findRules(MutableDataNodeRule.class).forEach(each -> each.put(logicDataSourceNames.iterator().next(), schemaName, tableName));
        }
        GenericSchemaBuilderMaterials materials = new GenericSchemaBuilderMaterials(database.getProtocolType(),
                database.getResource().getDatabaseType(), database.getResource().getDataSources(), database.getRuleMetaData().getRules(), props, schemaName);
        Map<String, ShardingSphereSchema> schemaMap = GenericSchemaBuilder.build(Collections.singletonList(tableName), materials);
        Optional<ShardingSphereTable> actualTableMetaData = Optional.ofNullable(schemaMap.get(schemaName)).map(optional -> optional.get(tableName));
        actualTableMetaData.ifPresent(optional -> {
            database.getSchema(schemaName).put(tableName, optional);
            federationDatabaseMetaData.putTable(schemaName, optional);
            optimizerPlanners.put(federationDatabaseMetaData.getName(), OptimizerPlannerContextFactory.create(federationDatabaseMetaData));
        });
    }
    
    private boolean containsInImmutableDataNodeContainedRule(final String tableName, final ShardingSphereDatabase database) {
        return database.getRuleMetaData().findRules(DataNodeContainedRule.class).stream()
                .filter(each -> !(each instanceof MutableDataNodeRule)).anyMatch(each -> each.getAllTables().contains(tableName));
    }
    
    @Override
    public String getType() {
        return RenameTableStatement.class.getName();
    }
}
