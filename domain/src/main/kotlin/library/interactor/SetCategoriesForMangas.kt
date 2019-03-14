/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.library.interactor

import io.reactivex.Single
import tachiyomi.core.db.Transaction
import tachiyomi.domain.category.model.MangaCategory
import tachiyomi.domain.category.repository.MangaCategoryRepository
import javax.inject.Inject
import javax.inject.Provider

class SetCategoriesForMangas @Inject constructor(
  private val mangaCategoryRepository: MangaCategoryRepository,
  private val transactions: Provider<Transaction>
) {

  fun interact(categoryIds: Collection<Long>, mangaIds: Collection<Long>) = Single.defer {
    val newMangaCategories = mutableListOf<MangaCategory>()
    for (categoryId in categoryIds) {
      // System categories don't need entries
      if (categoryId > 0) {
        for (mangaId in mangaIds) {
          val mangaCategory = MangaCategory(mangaId, categoryId)
          newMangaCategories.add(mangaCategory)
        }
      }
    }

    transactions.get()
      .withCompletable {
        mangaCategoryRepository.deleteForMangas(mangaIds)
          .andThen(mangaCategoryRepository.save(newMangaCategories))
      }
      .toSingle<Result> { Result.Success }
      .onErrorReturn(Result::InternalError)
  }

  sealed class Result {
    object Success : Result()
    data class InternalError(val error: Throwable) : Result()
  }

}