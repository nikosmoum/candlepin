/*
 * Default Candlepin rule set.
 */

var SOCKET_FACT="cpu.cpu_socket(s)";

function entitlement_name_space() {
    return Entitlement;
}

function consumer_delete_name_space() {
    return ConsumerDelete;
}

function pool_name_space() {
    return Pool;
}

function export_name_space() {
    return Export;
}

function compliance_name_space() {
	return Compliance;
}

/* Utility functions */
function contains(a, obj) {
    for (var i = 0; i < a.length; i++) {
        var result = a[i] == obj;
        if (result) {
            return true;
        }
    }
    return false;
}

function containsAll(a, b) {
    for (var i = 0 ; i < b.length ; i++) {
        if (!contains(a, b[i])) {
            return false;
        }
    }

    return true;
}

function getRelevantProvidedProducts(pool, products) {
    var provided = [];

    for (var i = 0 ; i < products.length ; i++) {
        var product = products[i];
        if (pool.provides(product.getId())) {
            provided.push(product);
        }
    }

    return provided;
}

function providesSameProducts(products1, products2) {
    return containsAll(products1, products2) && containsAll(products2, products1);
}

function arrayToString(a) {
    msg = "[";
    for each (q in a) {
        msg += q.getId() + " ";
    }
    msg += "]";
    return msg;
}

// XXX i don't know what this is really called
function recursiveCombination(a, n) {
    if (a.length == 0) {
        return [];
    }

    var res = [];
    for each (x in recursiveCombination(a.slice(1), n)) {
        if (x.length <= n) {
            res.push(x);
        }
        if (x.length + 1 <= n) {
            var z = x.slice(0);
            z.push(a[0]);
            res.push(z);
        }
    }
    res.push([a[0]]);
    return res;
}


// Check if the provided list of pools contains any duplicated products
// We don't need to worry about checking multi-entitle allowed products,
// as you can use as many of those as you want.
function hasNoProductOverlap(combination) {
    var seen_product_ids = [];
    for each (pool in combination) {
        var products = pool.products;
        for (var i = 0 ; i < products.length ; i++) {
            var product = products[i];
            if (!contains(seen_product_ids, product.id)) {
                seen_product_ids.push(product.id);
            } else if (product.getAttribute("multi-entitlement") != "yes") {
                return false;
            }
        }
    }

    return true;
}

function architectureMatches(product, consumer) {
    var supportedArches = [];
    var archString = product.getAttribute('arch');
    if (archString != null) {
        supportedArches = archString.toUpperCase().split(prodAttrSeparator);

        supportedArches = new java.util.HashSet(java.util.Arrays.asList(supportedArches));

        // If X86 is supported, add all variants to this list:
        if (supportedArches.contains("X86")) {
           supportedArches.add("I386");
           supportedArches.add("I586");
           supportedArches.add("I686");
        }

        if(!supportedArches.contains('ALL') &&
           (!consumer.hasFact("uname.machine")  ||
            !supportedArches.contains(consumer.getFact('uname.machine').toUpperCase())
            )
          ){
           return false;
       }
   }

   return true;
}

// Returns an array of the given pool, added to the array once for each entitlement
// we would need to satisfy the consumers socket requirements.
// TODO: rename this
function findStackingPools(pool, consumer, products) {
    var consumer_sockets = 1;
    if (consumer.hasFact(SOCKET_FACT)) {
        consumer_sockets = consumer.getFact(SOCKET_FACT);
    }
    
    log.debug("findStackingPools:");
    log.debug("  pool: " + pool.getId());
    log.debug("  stacking: " + pool.getProductAttribute("multi-entitlement"));

    var new_pools = [];
    if (pool.getProductAttribute("multi-entitlement") && pool.getProductAttribute("stacking_id")) {
        var product_sockets = 0;
        log.debug("  product: " +  pool.getProductId() + "is stackable and multi-entitled");
        log.debug("  each entitlement provides X sockets: " + pool.getProductAttribute("sockets"));
        log.debug("  consumer sockets: " + consumer_sockets);
        while (product_sockets < consumer_sockets) {
            new_pools.push(pool);
            product_sockets += parseInt(pool.getProductAttribute("sockets"));
        }
    }
    
    return new_pools;

}

function hasAllMultiEntitlement(combination) {
    for each (pool in combination) {
        var products = pool.products;
        for (var i = 0 ; i < products.length ; i++) {
            var product = products[i];
            if (product.getAttribute("multi-entitlement") != "yes") {
                return false;
            }
        }
    }
    return true;
}

// Splits the best pools array into two arrays, stacked pools and regular.
function splitStackingPools(combination) {
    var stackable_pools = [];
    var other_pools = [];
    for each (pool in combination) {
        log.debug("pool " + pool.getId());
        var products = pool.products;
        var stackable = false;
        for (var i = 0 ; i < products.length ; i++) {
            var product = products[i];
            log.debug("\nproduct " + product.getName());
            log.debug("multi-entitlement " + product.getAttribute("multi-entitlement") );
            log.debug("stacking_id " + product.getAttribute("stacking_id") );
            if ((product.getAttribute("multi-entitlement") == "yes") && (product.getAttribute("stacking_id"))){
                log.debug("this product is stackable");
                stackable = true;
            }
        }
        if (stackable) {
            stackable_pools.push(pool);
        } else {
            other_pools.push(pool);
        }
    }
    return {'stackable': stackable_pools, 'other': other_pools };
}

var Entitlement = {

    // defines mapping of product attributes to functions
    // the format is: <function name>:<order number>:<attr1>:...:<attrn>, comma-separated ex.:
    // func1:1:attr1:attr2:attr3, func2:2:attr3:attr4
    attribute_mappings: function() {
        return  "architecture:1:arch," +
            "sockets:1:sockets," +
            "requires_consumer_type:1:requires_consumer_type," +
            "user_license:1:user_license," +
            "virt_only:1:virt_only";
    },

    pre_virt_only: function() {
        var virt_pool = 'true'.equals(attributes.get('virt_only'));
        var guest = false;
        if (consumer.hasFact('virt.is_guest')) {
            guest = 'true'.equals(consumer.getFact('virt.is_guest'));
        }

        if (virt_pool && !guest) {
           pre.addError("rulefailed.virt.only");
        }
    },

    post_user_license: function() {
        // Default to using the same product from the pool.
        var productId = pool.getProductId();

        // Check if the sub-pool should be for a different product:
        if (attributes.containsKey("user_license_product")) {
            productId = attributes.get("user_license_product");
        }

        // Create a sub-pool for this user
        post.createUserRestrictedPool(productId, pool,
                                      attributes.get("user_license"));
    },

    pre_requires_consumer_type: function() {
        if (!attributes.get("requires_consumer_type").equals(consumer.getType())) {
            pre.addError("rulefailed.consumer.type.mismatch");
        }
    },

    pre_architecture: function() {
       if (!architectureMatches(product, consumer)) {
         pre.addWarning("rulewarning.architecture.mismatch");
       }
    },

    post_architecture: function() {
    },

    pre_sockets: function() {
        if (!consumer.hasFact(SOCKET_FACT) ||
            (parseInt(product.getAttribute("sockets")) < parseInt(consumer.getFact(SOCKET_FACT))) &&
            (!product.hasAttribute("stacking_id"))) {
            pre.addWarning("rulewarning.unsupported.number.of.sockets");
        }
    },

    post_sockets: function() {
    },

    pre_global: function() {
        if (consumer.hasEntitlement(pool.getId()) && product.getAttribute("multi-entitlement") != "yes") {
            pre.addError("rulefailed.consumer.already.has.product");
        }

		if (pre.getQuantity() > 1 && product.getAttribute("multi-entitlement") != "yes") {
			pre.addError("rulefailed.pool.does.not.support.multi-entitlement");
		}

		// If the product has no required consumer type, assume it is restricted to "system":
		if (!product.hasAttribute("requires_consumer_type")) {
			if (!consumer.getType().equals("system")) {
				pre.addError("rulefailed.consumer.type.mismatch");
			}

        }

        if (pool.getRestrictedToUsername() != null && !pool.getRestrictedToUsername().equals(consumer.getUsername())) {
            pre.addError("pool.not.available.to.user, pool= '" + pool.getRestrictedToUsername() + "', actual username='" + consumer.getUsername() + "'" );
        }

        // FIXME
        // for auto sub stacking, we need to be able to pull across multiple
        // pools eventually, so this would need to go away in that case
        pre.checkQuantity(pool);
    },

    post_global: function() {
    },

    select_pool_global: function() {
        // Greedy selection for now, in order
        // XXX need to watch out for multientitle products - how so?

        // An array of the preferred pool for each unique combination of provided products:
        var best_in_class_pools = [];

        // "pools" is a list of all the owner's pools which are compatible for the system:
        log.debug("Selecting best pools from: " + pools.length);
        for each (pool in pools) {
            log.debug("   " + pool.getId());
        }

        var consumer_sockets = 1;
        if (consumer.hasFact(SOCKET_FACT)) {
            consumer_sockets = consumer.getFact(SOCKET_FACT);
        }

        // Builds out the best_in_class_pools by iterating each pool, checking which products it provides (that 
        // are relevant to this request), then filtering out other pools which provide the *exact* same products
        // by selecting the preferred pool based on other criteria.
        for (var i = 0 ; i < pools.length ; i++) {
            var pool = pools[i];
        	log.debug("Checking pool for best unique provides combination: " + 
        			pool.getId());
            log.debug("  top level product: " + (pool.getTopLevelProduct().getId()));
            if (architectureMatches(pool.getTopLevelProduct(), consumer)) {
                var provided_products = getRelevantProvidedProducts(pool, products);
                log.debug("  relevant provided products: ");
                for each (pp in provided_products) {
                    log.debug("    " + pp.getId());
                }
                // XXX wasteful, should be a hash or something.
                // Tracks if we found another pool previously looked at which had the exact same provided products:
                var duplicate_found = false;

                // Check current pool against previous best to see if it's better:
                for each (best_pool in best_in_class_pools) {
                    var best_provided_products = getRelevantProvidedProducts(best_pool, products);

                    if (providesSameProducts(provided_products, best_provided_products)) {
                        duplicate_found = true;
                        log.debug("  provides same product combo as: " + pool.getId());

                        // Prefer a virt_only pool over a regular pool, else fall through to the next rules.
                        // At this point virt_only pools will have already been filtered out by the pre rules
                        // for non virt machines.
                        if (pool.getAttribute("virt_only") == "true" && best_pool.getAttribute("virt_only") != "true") {
                            best_in_class_pools[best_in_class_pools.indexOf(best_pool)] = pool;
                            log.debug("  replacing previous best due to virt-only");
                            break;
                        }
                        else if (best_pool.getAttribute("virt_only") == "true" && pool.getAttribute("virt_only") != "true") {
                            log.debug("  sticking with previous best due to virt-only");
                            break;
                        }

                        // If two pools are equal, select the pool that expires first
                        if (best_pool.getEndDate().after(pool.getEndDate())) {
                            best_in_class_pools[best_in_class_pools.indexOf(best_pool)] = pool;
                            log.debug("  replacing previous best due to earlier expiry date");
                            break;
                        }

                        // only if pool is new? aka, not in best pools yet
                        // TODO: this is inside a "providesSameProducts" check, how could this be new?
                        // <alikins_> guess the first duped pool that is not virt and the same pool
                        // TODO: this does not look right here, we're in a block that has already detected we provide the same products
                        // as something already in best pools, and yet we go and add this pool a bunch of times to the end of the array?
                        var new_pools = findStackingPools(pool, consumer, products);
                        best_in_class_pools.concat(new_pools);
                        for each (new_pool in new_pools){
                            log.debug("selecting new_pool: " + new_pool.getId());
                        }

                        log.debug("other");
                    }
                }

                // If we did not find a duplicate pool providing the same products, 
                if (!duplicate_found) {
                    log.debug("no duplicate");
                    var new_pools = findStackingPools(pool, consumer, products);
                    if (new_pools.length > 0){
                        log.debug("selecting new pools, no dups " + new_pools);
                        // TODO: what is happening here? we're completely overwriting the array of best_in_class_pools?
                        // should this be a concat?
                        // Suspect that if the early pools in the list are non-stackable, and a later pool providing 
                        // a different combo of products *is* stackable, we'll trigger this and blow away the previous 
                        // array of best in class. Needs testing.
                        best_in_class_pools = new_pools;
                        break;
                    }
                    else {
                        log.debug("selecting new pools, no new_pools length");
                        best_in_class_pools.push(pool);
                    }
                }
            }
        }


        var pools_info = splitStackingPools(best_in_class_pools);
        log.debug("pools_info stackable " + pools_info['stackable']);
        log.debug("pools_info other " + pools_info['other']);

        var candidate_combos = recursiveCombination(pools_info['other'], products.length)

        log.debug("Selecting " + products.length + " products from " + best_in_class_pools.length +
                  " pools in " + candidate_combos.length + " possible combinations");

        // Select the best pool combo. We prefer:
        // -The combo that provides the most products
        // -The combo that uses the fewest entitlements

        var selected_pools = [];
        selected_pools = best_in_class_pools;
        var best_provided_count = 0;

        for each (pool_combo in candidate_combos) {
            var provided_count = 0;
            var unique_provided = [];
            log.debug("checking pool_combo " + pool_combo);
            for each (pool in pool_combo) {
                log.debug("\tpool_combo " + pool.getId());
                var provided_products = getRelevantProvidedProducts(pool, products);
                for each (provided_product in provided_products) {
                    log.debug("\t\tprovided_product " + provided_product.getId());
                   if (provided_product.getAttribute("multi-entitlement")) {
                       log.debug("pool_combo select multi-entitlement");
                       unique_provided.push(provided_product);
                   }
                    // find all the pools that provide a product nothing else does
                    if (!contains(unique_provided, provided_product)) {
                        log.debug("pool_combo unique pools " + provided_product);
                        unique_provided.push(provided_product);
                    }
                }
            }

            for each (product in unique_provided){
                log.debug("unique_provided " + product.getId() + " " + product.getName());
            }
            // number of pools is less than the MIN pools
            // TODO: min pools?
            if (unique_provided.length < best_provided_count) {
                continue;
            } else if (unique_provided.length > best_provided_count || pool_combo.length < selected_pools.length) {
                if (hasNoProductOverlap(pool_combo)) {
                    selected_pools = pool_combo;
                    best_provided_count = unique_provided.length;
                }
                selected_pools = pool_combo;
            }
        }

        selected_pools.concat(pools_info['stackable']);

        for each (pool in selected_pools){
            log.debug("selected_pool2 " + pool + "  " + pool.getId());
        }

        // We may not have selected pools for all products; that's ok.
        if (selected_pools.length > 0) {
            return selected_pools;
        }

        return null;
    }
}

var ConsumerDelete = {
    global: function() {
        if (consumer.getType() == "person") {
            helper.deleteUserRestrictedPools(consumer.getUsername());
        }
    }
}

var Pool = {

    /*
     * Creates all appropriate pools for a subscription.
     */
    createPools: function () {
        var pools = new java.util.LinkedList();
        var quantity = sub.getQuantity() * sub.getProduct().getMultiplier();
        var providedProducts = new java.util.HashSet();
        var newPool = new org.fedoraproject.candlepin.model.Pool(sub.getOwner(), sub.getProduct().getId(),
                sub.getProduct().getName(), providedProducts,
                    quantity, sub.getStartDate(), sub.getEndDate(), sub.getContractNumber(),
                    sub.getAccountNumber());
        if (sub.getProvidedProducts() != null) {
            for each (var p in sub.getProvidedProducts().toArray()) {
                var providedProduct = new org.fedoraproject.candlepin.model.
                    ProvidedProduct(p.getId(), p.getName());
                providedProduct.setPool(newPool);
                providedProducts.add(providedProduct);
            }
        }
        helper.copyProductAttributesOntoPool(sub, newPool);
        newPool.setSubscriptionId(sub.getId());
        pools.add(newPool);

        // Check if we need to create a virt-only pool for this subscription:
        if (attributes.containsKey("virt_limit")) {
            var virt_limit = attributes.get("virt_limit");
            var virt_attributes = new java.util.HashMap();
            virt_attributes.put("virt_only", "true");
            virt_attributes.put("no_export", "true");
            // Make sure the virt pool does not have a virt_limit,
            // otherwise this will recurse infinitely
            virt_attributes.put("virt_limit", "0");

            if ('unlimited'.equals(virt_limit)) {
                pools.add(helper.createPool(sub, sub.getProduct().getId(),
                                            'unlimited', virt_attributes));
            } else {
                var virt_limit_quantity = parseInt(virt_limit);

                if (virt_limit_quantity > 0) {
                    var virt_quantity = sub.getQuantity() * virt_limit_quantity;

                    pools.add(helper.createPool(sub, sub.getProduct().getId(),
                                                virt_quantity.toString(), virt_attributes));
                }
            }
        }
        return pools;
    },

    /*
     * Updates the existing pools for a subscription.
     */
    updatePools: function () {
        var poolsUpdated = new java.util.LinkedList();
        for each (var existingPool in pools.toArray()) {
            log.info("Updating pool: " + existingPool.getId());
            var datesChanged = (!sub.getStartDate().equals(
                existingPool.getStartDate())) ||
                (!sub.getEndDate().equals(existingPool.getEndDate()));
            // Expected quantity is normally the subscription's quantity, but for
            // virt only pools we expect it to be sub quantity * virt_limit:
            var expectedQuantity = sub.getQuantity() * sub.getProduct().getMultiplier();
            if (existingPool.hasAttribute("virt_only") &&
                existingPool.getAttributeValue("virt_only").equals("true")) {
                // Assuming there mere be a virt limit attribute set:
                var virt_limit = attributes.get("virt_limit");

                if ('unlimited'.equals(virt_limit)) {
                    // Bad to hardcode this conversion here
                    // TODO:  Figure out a better way translate this value!
                    expectedQuantity = -1;
                } else {
                    expectedQuantity = sub.getQuantity() * parseInt(virt_limit);
                }
            }

            var quantityChanged = !(expectedQuantity == existingPool.getQuantity());
            var productsChanged = helper.checkForChangedProducts(existingPool, sub);

            var poolAttributesChanged = helper.copyProductAttributesOntoPool(sub,
                                                                             existingPool);
            if (poolAttributesChanged) {
                log.info("Updated pool attributes from subscription.");
            }

            if (!(quantityChanged || datesChanged || productsChanged ||
                  poolAttributesChanged)) {
                //TODO: Should we check whether pool is overflowing here?
                log.info("   No updates required.");
                continue;
            }

            if (quantityChanged) {
                log.info("   Quantity changed to: " + expectedQuantity);
                existingPool.setQuantity(expectedQuantity);
            }

            if (datesChanged) {
                log.info("   Subscription dates changed.");
                existingPool.setStartDate(sub.getStartDate());
                existingPool.setEndDate(sub.getEndDate());
            }

            if (productsChanged) {
                log.info("   Subscription products changed.");
                existingPool.setProductName(sub.getProduct().getName());
                existingPool.getProvidedProducts().clear();

                if (sub.getProvidedProducts() != null) {
                    for each (var p in sub.getProvidedProducts().toArray()) {
                        var providedProduct = new org.fedoraproject.candlepin.model.
                            ProvidedProduct(p.getId(), p.getName());
                        existingPool.addProvidedProduct(providedProduct);
                    }
                }
            }
            poolsUpdated.add(new org.fedoraproject.candlepin.policy.js.pool.PoolUpdate(
                                 existingPool, datesChanged, quantityChanged, productsChanged));
        }
        return poolsUpdated;
    }

}

var Export = {
    can_export_entitlement: function() {
        no_export = attributes.containsKey('no_export') &&
                    'true'.equals(attributes.get('no_export'));

        return !consumer.getType().getLabel().equals("candlepin") || !no_export;
    }
}

function is_stacked(ent) {
    return ent.getPool().hasProductAttribute("stacking_id");
}

/**
 * Check the given list of entitlements to see if a stack ID is compliant for
 * a consumer's socket count.
 */
function stack_is_compliant(consumer, stack_id, ents, log) {
    log.debug("Checking stack compliance for: " + stack_id);
    var consumer_sockets = 1;
    if (consumer.hasFact(SOCKET_FACT)) {
        consumer_sockets = parseInt(consumer.getFact(SOCKET_FACT));
    }
    log.debug("Consumer sockets: " + consumer_sockets);

    var covered_sockets = 0;
    for each (var ent in ents.toArray()) {
        var currentStackId = ent.getPool().getProductAttribute("stacking_id").getValue();
        if (is_stacked(ent) && currentStackId.equals(stack_id)) {
            covered_sockets += parseInt(ent.getPool().getProductAttribute("sockets").getValue()) * ent.getQuantity();
            log.debug("Ent " + ent.getId() + " took covered sockets to: " + covered_sockets);
        }
    }

    return covered_sockets >= consumer_sockets;
}

var Compliance = {

    /**
     * Checks compliance status for a consumer on a given date.
     */
    get_status: function() {
        var status = new org.fedoraproject.candlepin.policy.js.compliance.ComplianceStatus(ondate);

        // Track the stack IDs we've already checked to save some time:
        var compliant_stack_ids = new java.util.HashSet();
        var non_compliant_stack_ids = new java.util.HashSet();

        // Loop through the consumers installed products and check compliance for each.
        for each (var installedProd in consumer.getInstalledProducts().toArray()) {
            var installedPid = installedProd.getProductId();
            log.debug("Checking compliance for installed product:" + installedPid);
            for each (var e in entitlements.toArray()) {
                if (e.getPool().provides(installedPid) == true) {
                    log.debug("  " + e.getPool().getId() + " provides");

                    // If the pool is stacked we need to now check if the stack
                    // requirements are met to be considered compliant:
                    if (is_stacked(e)) {
                        var stackId = e.getPool().getProductAttribute("stacking_id").getValue();
                        log.debug("  pool has stack ID: " + stackId);

                        // Shortcuts for stacks we've already checked:
                        if (non_compliant_stack_ids.contains(stackId) > 0) {
                            log.debug("  stack already found to be non-compliant");
                            status.addPartiallyCompliantProduct(installedPid, e);
                        }
                        else if (compliant_stack_ids.contains(stackId) > 0) {
                            log.debug("  stack already found to be non-compliant");
                            status.addCompliantProduct(installedPid, e);
                        }
                        // Otherwise check the stack and add appropriately:
                        else if(stack_is_compliant(consumer, stackId, entitlements, log)) {
                            log.debug("  stack is compliant");
                            status.addCompliantProduct(installedPid, e);
                            compliant_stack_ids.add(stackId);
                        }
                        else {
                            log.debug("  stack is non-compliant");
                            status.addPartiallyCompliantProduct(installedPid, e);
                            non_compliant_stack_ids.add(stackId);
                        }
                    }
                    // Not stacked, just a regular entitlement means we're compliant:
                    else {
                        status.addCompliantProduct(installedPid, e);
                    }
                }
            }

            // Not compliant if we didn't find any entitlements for this product:
            if (!status.getCompliantProducts().containsKey(installedPid) &&
                !status.getPartiallyCompliantProducts().containsKey(installedPid)) {
                log.debug("  nothing provides.");
                status.addNonCompliantProduct(installedPid);
            }
        }

        // TODO: We've checked installed products, but the consumer may have manually
        // subscribed to a stacked product without consuming sufficient entitlments
        // to meet the stacking requirements. Even though the product is not installed,
        // this should still be considered non-compliance and need to be fixed by grabbing
        // more entitlements for the stack.

        return status;
    }

}
