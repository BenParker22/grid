package lib.usagerights

import com.gu.mediaservice.lib.config.UsageRightsConfig
import com.gu.mediaservice.model._
import lib.UsageQuota


trait CostCalculator {
  import UsageRightsConfig.{freeSuppliers, suppliersCollectionExcl}

  val defaultCost = Pay

  def getCost(supplier: String, collection: Option[String]): Option[Cost] = {
      val free = isFreeSupplier(supplier) && ! collection.exists(isExcludedColl(supplier, _))
      if (free) Some(Free) else None
  }

  def isPay(usageRights: UsageRights): Boolean =
    getCost(usageRights) == Pay

  def getOverQuota(usageRights: UsageRights) =
    if (UsageQuota.isOverQuota(usageRights)) {
      Some(Overquota)
    } else {
      None
    }

  def getCost(usageRights: UsageRights): Cost = {
      val restricted  : Option[Cost] = usageRights.restrictions.map(r => Conditional)
      val categoryCost: Option[Cost] = usageRights.defaultCost
      val overQuota: Option[Cost] = getOverQuota(usageRights)
      val supplierCost: Option[Cost] = usageRights match {
        case u: Agency => getCost(u.supplier, u.suppliersCollection)
        case _ => None
      }

      restricted
        .orElse(overQuota)
        .orElse(categoryCost)
        .orElse(supplierCost)
        .getOrElse(defaultCost)
  }

  private def isFreeSupplier(supplier: String) = freeSuppliers.contains(supplier)

  private def isExcludedColl(supplier: String, supplierColl: String) =
    suppliersCollectionExcl.get(supplier).exists(_.contains(supplierColl))
}
