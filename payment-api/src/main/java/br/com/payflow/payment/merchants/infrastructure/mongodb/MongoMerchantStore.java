package br.com.payflow.payment.merchants.infrastructure.mongodb;

import java.util.Optional;

import br.com.payflow.payment.merchants.authenticate.MerchantLookup;
import br.com.payflow.payment.merchants.domain.Merchant;
import br.com.payflow.payment.merchants.provision.MerchantProvisioning;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

// One MongoDB adapter for the merchants collection, exposed to each slice through its own port.
public final class MongoMerchantStore implements MerchantProvisioning, MerchantLookup {

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

    @Override
    public Optional<Merchant> findByApiKeyHash(String apiKeyHash) {
        return Optional.ofNullable(mongo.findOne(
                Query.query(Criteria.where("apiKeyHash").is(apiKeyHash)), MerchantDocument.class))
                .map(MerchantMapper::toDomain);
    }
}
