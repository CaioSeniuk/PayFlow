package br.com.payflow.payment.infrastructure.persistence.mongodb;

import br.com.payflow.payment.application.MerchantStore;
import br.com.payflow.payment.domain.Merchant;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public final class MongoMerchantStore implements MerchantStore {

    private final MongoTemplate mongo;

    public MongoMerchantStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Merchant insertIfAbsent(Merchant candidate) {
        MerchantDocument document = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(candidate.id())),
                new Update().setOnInsert("name", candidate.name())
                        .setOnInsert("apiKeyHash", candidate.apiKeyHash())
                        .setOnInsert("createdAt", candidate.createdAt()),
                FindAndModifyOptions.options().upsert(true).returnNew(true), MerchantDocument.class);
        return MerchantMapper.toDomain(document);
    }
}
