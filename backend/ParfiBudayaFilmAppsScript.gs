const SPREADSHEET_ID = "1H_lC5ele6KImGKmKBmrIqZaZCYS2d2aFRRQGujKaVRw";

const SHEET_MEMBERS = "admin";
const SHEET_FILMS = "films";
const SHEET_PORTAL = "portal_posts";
const SHEET_PORTAL_REPORTS = "portal_reports";
const SHEET_VIDEO_COLLECTIONS = "video_collections";

const FILM_COVER_FOLDER_ID = "19k7_QFBh4MSLdU9MLF0X0szlixQxQShz";
const PORTAL_POSTER_FOLDER_ID = "1fUeJloy5cHoY44cbov080jodLzRhvJ57";
const TRAILER_FOLDER_ID = "13se70xmVlwOldUG7bNzpDp9Ly6vUMR78";
const API_VERSION = "2026-08-04";

const MEMBER_HEADERS = ["username", "password", "status"];
const FILM_HEADERS = ["id", "Judul", "Rilis", "Kategori", "Kreator", "synopsis", "poster_url", "trailer_url", "movie_url", "status", "views", "likes"];
const PORTAL_HEADERS = ["id", "user_id", "Kreator", "Kategori", "Poster_Url", "Status", "Created_At"];
const PORTAL_REPORT_HEADERS = ["id", "post_id", "report_reason", "reported_link", "status"];
const VIDEO_COLLECTION_HEADERS = ["id", "judul", "kategori", "nama_channel", "playlist_url", "status"];

function doGet(e) {
  try {
    const type = String((e && e.parameter && e.parameter.type) || "").trim();
    if (type === "setup") return jsonOutput_(setupParfiSheets());
    if (type === "health") {
      return jsonOutput_({
        success: true,
        service: "Cinema PARFI Jatim",
        version: API_VERSION
      });
    }
    if (type === "video_collections") return jsonOutput_({ success: true, collections: getActiveVideoCollections_() });
    if (type === "portal_posts") return jsonOutput_({ success: true, portal_posts: getActivePortalPosts_() });
    if (type && type !== "get_films" && type !== "films" && type !== "movies" && type !== "songs") {
      return jsonOutput_({ success: false, message: "Tipe GET tidak dikenal." });
    }
    const movies = getApprovedMovies_();
    const categories = getApprovedCategories_(movies);
    return jsonOutput_({ success: true, movies: movies, films: movies, songs: movies, categories: categories });
  } catch (error) {
    return jsonOutput_({ success: false, message: safeErrorMessage_(error, "Unhandled server error (GET).") });
  }
}

function doPost(e) {
  try {
    setupParfiSheets();
    const payload = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const type = String(payload.type || "").trim();

    if (type === "hub_register" || type === "portal_register" || type === "hub_update" || type === "admin_update") return handleMemberRegister_(payload);
    if (type === "hub_login") return handleMemberLogin_(payload);
    if (type === "hub_check_status") return handleMemberCheckStatus_(payload);
    if (type === "film_registration" || type === "song_registration") return handleFilmRegistration_(payload);
    if (type === "film_update") return handleFilmUpdate_(payload);
    if (type === "film_delete") return handleFilmDelete_(payload);
    if (type === "film_like") return handleFilmLike_(payload);
    if (type === "film_view") return handleFilmView_(payload);
    if (type === "portal_post") return handlePortalPost_(payload);
    if (type === "portal_post_delete" || type === "portal_delete") return handlePortalPostDelete_(payload);
    if (type === "portal_report" || type === "content_report") return handlePortalReport_(payload);

    return jsonOutput_({ success: false, message: "Tipe request tidak dikenal." });
  } catch (error) {
    return jsonOutput_({ success: false, message: safeErrorMessage_(error, "Unhandled server error.") });
  }
}

function setupParfiSheets() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  ensureSheetWithHeaders_(ss, SHEET_MEMBERS, MEMBER_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);
  ensureVideoCollectionsSheet_(ss);
  repairFilmHeaderAliases_(ss);
  return { success: true, message: "PARFI sheets siap." };
}

function ensureVideoCollectionsSheet_(ss) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_VIDEO_COLLECTIONS, VIDEO_COLLECTION_HEADERS);
  if (sheet.getLastRow() > 1) return sheet;
  sheet.getRange(2, 1, 3, VIDEO_COLLECTION_HEADERS.length).setValues([
    ["collection_ketoprak_humor", "Ketoprak Humor", "Seni dan Budaya", "LUCU AJA BOLEH", "https://youtube.com/playlist?list=PLDD9P84T2kDvEBTGJS0yfGHa-akPfMjcT", "ACTIVE"],
    ["collection_warkop_dki", "Warkop DKI", "Klasik", "Rumah Film Indonesia", "https://youtube.com/playlist?list=PLm43CaIKyujQtw2qkAYR498xuJls2llOJ", "ACTIVE"],
    ["collection_kadir_doyok", "Kadir Doyok", "Klasik", "Tirta Sagara", "https://youtube.com/playlist?list=PLvF4zfFegisdotb7_dnLoNr0VmYYiBtQ1", "ACTIVE"]
  ]);
  return sheet;
}

function getActiveVideoCollections_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureVideoCollectionsSheet_(ss);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];
  const col = getSheetHeaderMap_(values[0]);
  return values.slice(1)
    .map(function(row) {
      return {
        id: String(row[col["id"]] || "").trim(),
        title: String(row[col["judul"]] || "").trim(),
        category: String(row[col["kategori"]] || "").trim(),
        channel_name: String(row[col["nama_channel"]] || "").trim(),
        playlist_url: String(row[col["playlist_url"]] || "").trim(),
        status: String(row[col["status"]] || "").trim()
      };
    })
    .filter(function(item) {
      return item.title && item.playlist_url && String(item.status).toUpperCase() === "ACTIVE";
    });
}

function handleMemberRegister_(payload) {
  const user = registerOrLoginMember_(payload);
  return jsonOutput_({ success: true, message: "Admin berhasil disimpan.", user: user });
}

function handleMemberLogin_(payload) {
  const username = normalizeAdminValue_(payload.username || payload.user || payload.whatsapp || payload.phone || payload.noWa);
  const password = normalizeAdminValue_(payload.password || payload.pass || "");
  const user = findMemberByCredentials_(username, password);
  if (!user) return jsonOutput_({ success: false, status: "PENDING", message: "Akun admin tidak ditemukan." });
  if (normalizeAdminStatus_(user.status) !== "ACTIVE") {
    return jsonOutput_({ success: false, status: normalizeAdminStatus_(user.status), message: "Akun admin tidak aktif.", user: user });
  }
  return jsonOutput_({ success: true, status: "ACTIVE", user: user });
}

function handleMemberCheckStatus_(payload) {
  const username = normalizeAdminValue_(payload.username || payload.user || payload.whatsapp || payload.phone || payload.noWa);
  const password = normalizeAdminValue_(payload.password || payload.pass || "");
  const user = findMemberByCredentials_(username, password);
  if (!user) return jsonOutput_({ success: false, status: "PENDING", message: "Info akun admin tidak ditemukan." });
  return jsonOutput_({ success: true, status: normalizeAdminStatus_(user.status), message: "Status ditemukan.", user: user });
}

function handleFilmRegistration_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);

  let posterUrl = normalizePublicUrl_(String(payload.poster_url || payload.coverArtworkLink || payload.posterUrl || "").trim());
  if (payload.coverImageBase64) {
    posterUrl = uploadBase64ImageToDrive_(
      payload.coverImageBase64,
      payload.coverImageFileName || "cover.jpg",
      payload.coverImageMimeType || "image/jpeg",
      FILM_COVER_FOLDER_ID
    );
  }

  let trailerUrl = normalizePublicUrl_(String(payload.trailer_url || payload.trailerUrl || "").trim());
  if (payload.trailerFileBase64) {
    trailerUrl = uploadBase64FileToDrive_(
      payload.trailerFileBase64,
      payload.trailerFileName || "trailer.mp4",
      payload.trailerFileMimeType || "video/mp4",
      TRAILER_FOLDER_ID
    );
  }

  sheet.appendRow([
    "film_" + Date.now(),
    String(payload.title || payload.songTitle || "").trim(),
    String(payload.release || payload.Rilis || "").trim(),
    String(payload.category || payload.Kategori || "").trim(),
    String(payload.kreator || payload.Kreator || payload.creator || payload.sutradara || payload.Sutradara || payload.director || payload.staring || payload.Staring || payload.castName || "").trim(),
    String(payload.synopsis || payload.description || "").trim(),
    posterUrl,
    trailerUrl,
    normalizePublicUrl_(String(payload.movie_url || payload.video_url || payload.videoUrl || payload.mp3Link || "").trim()),
    "PENDING",
    0,
    0
  ]);

  return jsonOutput_({ success: true, message: "Registrasi film berhasil disimpan. Tunggu review admin." });
}

function handleFilmLike_(payload) {
  const filmId = String(payload.film_id || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });
  const user = validateActiveMember_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return jsonOutput_({ success: false, message: "Data film kosong." });

  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  const likesIdx = col["likes"];
  if (idIdx === undefined || likesIdx === undefined) return jsonOutput_({ success: false, message: "Kolom id/likes tidak ditemukan." });

  const lock = LockService.getDocumentLock();
  lock.waitLock(20000);
  try {
    const key = "film_like__" + filmId + "__" + user.id;
    const prop = PropertiesService.getDocumentProperties();
    const already = prop.getProperty(key) === "1";
    let totalLikes = 0;
    for (let r = 1; r < values.length; r++) {
      if (String(values[r][idIdx] || "").trim() === filmId) {
        const currentLikes = Number(values[r][likesIdx] || 0);
        totalLikes = already ? currentLikes : currentLikes + 1;
        if (!already) sheet.getRange(r + 1, likesIdx + 1).setValue(totalLikes);
        break;
      }
    }
    if (!already) prop.setProperty(key, "1");
    return jsonOutput_({ success: true, total_likes: totalLikes, likes: totalLikes, already_liked: true });
  } finally {
    lock.releaseLock();
  }
}

function handleFilmView_(payload) {
  const filmId = String(payload.film_id || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return jsonOutput_({ success: false, message: "Data film kosong." });

  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  const viewsIdx = col["views"];
  if (idIdx === undefined || viewsIdx === undefined) return jsonOutput_({ success: false, message: "Kolom id/views tidak ditemukan." });

  for (let r = 1; r < values.length; r++) {
    if (String(values[r][idIdx] || "").trim() === filmId) {
      const nextViews = Number(values[r][viewsIdx] || 0) + 1;
      sheet.getRange(r + 1, viewsIdx + 1).setValue(nextViews);
      return jsonOutput_({ success: true, views: nextViews });
    }
  }
  return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
}

function handlePortalPost_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  if (!payload.posterImageBase64) throw new Error("posterImageBase64 is required.");

  const posterUrl = uploadBase64ImageToDrive_(
    payload.posterImageBase64,
    payload.posterImageFileName || "poster.jpg",
    payload.posterImageMimeType || "image/jpeg",
    PORTAL_POSTER_FOLDER_ID
  );

  sheet.appendRow([
    "portal_" + Date.now(),
    String(payload.userId || payload.username || payload.bandName || "guest").trim(),
    String(payload.bandName || payload.username || payload.creator || "Guest").trim(),
    String(payload.category || "").trim(),
    posterUrl,
    "PENDING",
    new Date()
  ]);
  return jsonOutput_({ success: true, message: "Poster berhasil disimpan dan menunggu review admin." });
}

function handlePortalReport_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);

  const postId = String(payload.postId || payload.targetId || "").trim();
  const reason = String(payload.reason || "").trim();
  if (!postId) return jsonOutput_({ success: false, message: "post_id wajib diisi." });
  if (!reason) return jsonOutput_({ success: false, message: "report_reason wajib diisi." });

  sheet.appendRow([
    "report_" + Date.now(),
    postId,
    reason,
    String(payload.reportedLink || "").trim(),
    "NEW"
  ]);
  return jsonOutput_({ success: true, message: "Laporan diterima. Terima kasih." });
}

function handleFilmDelete_(payload) {
  const admin = validateActiveMember_(payload);
  const filmId = String(payload.film_id || payload.id || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const deleted = deleteRowById_(sheet, filmId);
  if (!deleted) return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
  return jsonOutput_({ success: true, message: "Film berhasil dihapus.", admin: admin });
}

function handleFilmUpdate_(payload) {
  const admin = validateActiveMember_(payload);
  const filmId = String(payload.film_id || payload.id || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return jsonOutput_({ success: false, message: "Data film kosong." });

  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  if (idIdx === undefined) return jsonOutput_({ success: false, message: "Kolom id tidak ditemukan." });

  for (let r = 1; r < values.length; r++) {
    if (String(values[r][idIdx] || "").trim() === filmId) {
      const nextTitle = String(payload.title || payload.judul || values[r][col["Judul"]] || "").trim();
      const nextRelease = String(payload.release || payload.rilis || values[r][col["Rilis"]] || "").trim();
      const nextCategory = String(payload.category || payload.kategori || values[r][col["Kategori"]] || "").trim();
      const nextKreator = String(payload.kreator || payload.creator || payload.director || payload.sutradara || values[r][col["Kreator"]] || "").trim();
      const nextSynopsis = String(payload.synopsis || payload.description || values[r][col["synopsis"]] || "").trim();
      const nextPoster = normalizePublicUrl_(String(payload.poster_url || payload.posterUrl || values[r][col["poster_url"]] || "").trim());
      const nextTrailer = normalizePublicUrl_(String(payload.trailer_url || payload.trailerUrl || values[r][col["trailer_url"]] || "").trim());
      const nextMovie = normalizePublicUrl_(String(payload.movie_url || payload.video_url || payload.videoUrl || values[r][col["movie_url"]] || "").trim());

      if (col["Judul"] !== undefined) sheet.getRange(r + 1, col["Judul"] + 1).setValue(nextTitle);
      if (col["Rilis"] !== undefined) sheet.getRange(r + 1, col["Rilis"] + 1).setValue(nextRelease);
      if (col["Kategori"] !== undefined) sheet.getRange(r + 1, col["Kategori"] + 1).setValue(nextCategory);
      if (col["Kreator"] !== undefined) sheet.getRange(r + 1, col["Kreator"] + 1).setValue(nextKreator);
      if (col["synopsis"] !== undefined) sheet.getRange(r + 1, col["synopsis"] + 1).setValue(nextSynopsis);
      if (col["poster_url"] !== undefined) sheet.getRange(r + 1, col["poster_url"] + 1).setValue(nextPoster);
      if (col["trailer_url"] !== undefined) sheet.getRange(r + 1, col["trailer_url"] + 1).setValue(nextTrailer);
      if (col["movie_url"] !== undefined) sheet.getRange(r + 1, col["movie_url"] + 1).setValue(nextMovie);
      if (col["status"] !== undefined && String(payload.status || "").trim()) {
        sheet.getRange(r + 1, col["status"] + 1).setValue(String(payload.status).trim());
      }
      return jsonOutput_({ success: true, message: "Film berhasil diperbarui.", admin: admin });
    }
  }
  return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
}

function handlePortalPostDelete_(payload) {
  const admin = validateActiveMember_(payload);
  const postId = String(payload.post_id || payload.postId || payload.id || "").trim();
  if (!postId) return jsonOutput_({ success: false, message: "post_id wajib diisi." });
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const deleted = deleteRowById_(sheet, postId);
  if (!deleted) return jsonOutput_({ success: false, message: "Poster tidak ditemukan." });
  return jsonOutput_({ success: true, message: "Poster berhasil dihapus.", admin: admin });
}

function getApprovedMovies_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  return readSheetObjects_(sheet)
    .filter(function(movie) {
      const s = String(movie.status || "").trim().toLowerCase();
      return s === "approved" || s === "active";
    })
    .map(function(movie) {
      const id = String(movie.id || "").trim();
      const title = String(movie.Judul || "").trim();
      const category = String(movie.Kategori || "").trim();
      const synopsis = String(movie.synopsis || "").trim();
      const poster = normalizePublicUrl_(String(movie.poster_url || "").trim());
      const trailer = normalizePublicUrl_(String(movie.trailer_url || "").trim());
      const movieUrl = normalizePublicUrl_(String(movie.movie_url || "").trim());
      const kreator = String(movie.Kreator || movie.kreator || movie.creator || movie.Sutradara || movie.sutradara || movie.director || movie.Staring || movie.staring || "").trim();
      const release = String(movie.Rilis || "").trim();
      const status = String(movie.status || "").trim();
      const views = Number(movie.views || 0);
      const likes = Number(movie.likes || 0);
      return {
        id: id,
        film_id: id,
        title: title,
        genre: category,
        category: category,
        duration: release,
        kreator: kreator,
        creator: kreator,
        artist: kreator,
        director: kreator,
        cast_name: kreator,
        synopsis: synopsis,
        description: synopsis,
        poster_url: poster,
        cover_url: poster,
        trailer_url: trailer,
        movie_url: movieUrl,
        video_url: movieUrl,
        status: status,
        featured: "no",
        views: views,
        likes: likes,
        already_liked: false
      };
    });
}

function getApprovedCategories_(movies) {
  const set = {};
  movies.forEach(function(movie) {
    const c = String(movie.category || "").trim();
    if (c) set[c] = true;
  });
  return Object.keys(set).sort();
}

function getActivePortalPosts_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  return readSheetObjects_(sheet)
    .filter(function(post) { return String(post.Status || "").trim().toUpperCase() === "ACTIVE"; })
    .map(function(post) {
      const creator = String(post.Kreator || "").trim();
      return {
        id: String(post.id || "").trim(),
        title: creator || "Poster Board",
        band_name: creator,
        creator_name: creator,
        submitter_name: creator,
        category: String(post.Kategori || "").trim() || "Poster",
        poster_url: String(post.Poster_Url || "").trim(),
        target_url: "",
        short_caption: "",
        caption: "",
        event_date: "",
        status: String(post.Status || "").trim(),
        created_at: formatDateValue_(post["Created_At"])
      };
    });
}

function registerOrLoginMember_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_MEMBERS, MEMBER_HEADERS);
  const username = normalizeAdminValue_(payload.username || payload.user || payload.bandName || payload.creator || payload.Kreator || "");
  const password = normalizeAdminValue_(payload.password || payload.pass || payload.city || payload["Asal Kota"] || "");
  const existing = findMemberByCredentials_(username, password);
  if (existing) return existing;

  const hasExistingRow = sheet.getLastRow() > 1;
  const existingStatus = hasExistingRow ? normalizeAdminStatus_(sheet.getRange(2, 3).getValue()) : getAdminDefaults_().status;
  const nextUsername = username || (hasExistingRow ? normalizeAdminValue_(sheet.getRange(2, 1).getValue()) : getAdminDefaults_().username);
  const nextPassword = password || (hasExistingRow ? normalizeAdminValue_(sheet.getRange(2, 2).getValue()) : getAdminDefaults_().password);
  if (sheet.getLastRow() <= 1) {
    sheet.appendRow([nextUsername, nextPassword, existingStatus]);
  } else {
    sheet.getRange(2, 1, 1, 3).setValues([[nextUsername, nextPassword, existingStatus]]);
  }
  return buildAdminUser_(nextUsername, nextPassword, existingStatus);
}

function validateActiveMember_(payload) {
  const username = normalizeAdminValue_(payload.username || payload.user || payload.bandName || payload.creator || payload.Kreator || payload.userId || "");
  const password = normalizeAdminValue_(payload.password || payload.pass || payload.city || payload["Asal Kota"] || payload.whatsapp || payload.Whatsapp || "");
  const user = findMemberByCredentials_(username, password);
  if (!user) throw new Error("Akun admin tidak ditemukan.");
  if (normalizeAdminStatus_(user.status) !== "ACTIVE") throw new Error("Akun admin tidak aktif.");
  return user;
}

function getAdminDefaults_() {
  return { username: "admin", password: "admin", status: "ACTIVE" };
}

function normalizeAdminValue_(value) {
  return String(value == null ? "" : value).trim();
}

function normalizeAdminStatus_(value) {
  const status = normalizeAdminValue_(value).toUpperCase();
  if (status === "PENDING") return "PENDING";
  if (status === "BLOCKED") return "PENDING";
  return "ACTIVE";
}

function buildAdminUser_(username, password, status) {
  const normalizedUsername = normalizeAdminValue_(username);
  const normalizedPassword = normalizeAdminValue_(password);
  return {
    id: "admin_" + (normalizedUsername || "admin"),
    userId: normalizedUsername || "admin",
    bandName: normalizedUsername || "admin",
    creator: normalizedUsername || "admin",
    username: normalizedUsername,
    password: normalizedPassword,
    city: "",
    whatsapp: "",
    createdAt: "",
    status: normalizeAdminStatus_(status)
  };
}

function findMemberByCredentials_(username, password) {
  const targetUsername = normalizeAdminValue_(username);
  const targetPassword = normalizeAdminValue_(password);
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_MEMBERS, MEMBER_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  for (let r = 1; r < values.length; r++) {
    const rowUsername = normalizeAdminValue_(getRowValueByHeader_(values[r], col, "username"));
    const rowPassword = normalizeAdminValue_(getRowValueByHeader_(values[r], col, "password"));
    if (rowUsername === targetUsername && rowPassword === targetPassword) {
      return mapAdminFromSheetRow_(sheet, values[r], r + 1, col);
    }
  }
  if (targetUsername === getAdminDefaults_().username && targetPassword === getAdminDefaults_().password && values.length <= 1) {
    return buildAdminUser_(targetUsername, targetPassword, getAdminDefaults_().status);
  }
  return null;
}

function findMemberById_(id) {
  return findMemberByCredentials_(normalizeAdminValue_(id), "");
}

function mapMemberFromSheetRow_(sheet, row, rowNumber, col) {
  return mapAdminFromSheetRow_(sheet, row, rowNumber, col);
}

function normalizeWhatsapp(number) {
  return normalizeAdminValue_(number);
}

function normalizeWhatsappSafe_(number) {
  return normalizeAdminValue_(number);
}

function normalizeMemberStatus_(value) {
  const s = String(value || "").trim().toUpperCase();
  if (s === "ACTIVE" || s === "ADMIN") return "ACTIVE";
  if (s === "BLOCKED") return "BLOCKED";
  return "PENDING";
}

function uploadBase64ImageToDrive_(base64, fileName, mimeType, folderId) {
  return uploadBase64FileToDrive_(base64, fileName, mimeType, folderId);
}

function uploadBase64FileToDrive_(base64, fileName, mimeType, folderId) {
  try {
    const folder = DriveApp.getFolderById(folderId);
    const cleanBase64 = String(base64 || "").replace(/^data:.*;base64,/, "");
    const bytes = Utilities.base64Decode(cleanBase64);
    const blob = Utilities.newBlob(bytes, mimeType, fileName);
    const file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    return "https://drive.google.com/uc?export=view&id=" + file.getId();
  } catch (error) {
    return "https://via.placeholder.com/720x1024/1A1A1A/FFE0A3?text=Poster+Board";
  }
}

function diagnoseDriveAccess() {
  try {
    const portalFolder = DriveApp.getFolderById(PORTAL_POSTER_FOLDER_ID);
    const coverFolder = DriveApp.getFolderById(FILM_COVER_FOLDER_ID);
    const trailerFolder = DriveApp.getFolderById(TRAILER_FOLDER_ID);
    return {
      success: true,
      message: "DriveApp OK",
      portalFolderName: portalFolder.getName(),
      portalFolderId: PORTAL_POSTER_FOLDER_ID,
      filmCoverFolderName: coverFolder.getName(),
      filmCoverFolderId: FILM_COVER_FOLDER_ID,
      trailerFolderName: trailerFolder.getName(),
      trailerFolderId: TRAILER_FOLDER_ID
    };
  } catch (error) {
    return { success: false, message: safeErrorMessage_(error, "DriveApp belum punya izin.") };
  }
}

function getSheetHeaderMap_(headersRow) {
  const col = {};
  headersRow.forEach(function(header, index) { col[String(header || "").trim()] = index; });
  return col;
}

function getRowValueByHeader_(row, col, headerName) {
  if (!Object.prototype.hasOwnProperty.call(col, headerName)) return "";
  return row[col[headerName]];
}

function getOrRepairMemberId_(sheet, row, rowNumber, col) {
  const idIdx = col["username"];
  if (idIdx === undefined) throw new Error("Kolom username tidak ditemukan di admin.");
  const current = String(row[idIdx] || "").trim();
  if (current) return current;
  const next = getAdminDefaults_().username;
  sheet.getRange(rowNumber, idIdx + 1).setValue(next);
  row[idIdx] = next;
  return next;
}

function getOrRepairMemberCreatedAt_(sheet, row, rowNumber, col) {
  return "";
}

function mapAdminFromSheetRow_(sheet, row, rowNumber, col) {
  const username = normalizeAdminValue_(getRowValueByHeader_(row, col, "username")) || getAdminDefaults_().username;
  const password = normalizeAdminValue_(getRowValueByHeader_(row, col, "password")) || getAdminDefaults_().password;
  const status = normalizeAdminStatus_(getRowValueByHeader_(row, col, "status"));
  return buildAdminUser_(username, password, status);
}

function ensureSheetWithHeaders_(ss, sheetName, headers) {
  const sheet = ss.getSheetByName(sheetName) || ss.insertSheet(sheetName);
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
    return sheet;
  }
  const existing = sheet.getRange(1, 1, 1, headers.length).getValues()[0];
  const mismatch = headers.some(function(h, i) { return String(existing[i] || "").trim() !== h; });
  if (mismatch) sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  return sheet;
}

function repairFilmHeaderAliases_(ss) {
  const sheet = ss.getSheetByName(SHEET_FILMS);
  if (!sheet || sheet.getLastRow() < 1) return;
  const headers = sheet.getRange(1, 1, 1, Math.max(sheet.getLastColumn(), FILM_HEADERS.length)).getValues()[0];
  const fixed = headers.map(function(header) {
    const value = String(header || "").trim();
    if (value === "Staring" || value === "staring" || value === "Sutradara" || value === "sutradara") return "Kreator";
    return value;
  });
  const desired = FILM_HEADERS.slice();
  let changed = false;
  for (let i = 0; i < desired.length; i++) {
    if (String(fixed[i] || "").trim() !== desired[i]) {
      changed = true;
      break;
    }
  }
  if (changed) {
    sheet.getRange(1, 1, 1, desired.length).setValues([desired]);
  }
}

function deleteRowById_(sheet, id) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return false;
  const headers = getSheetHeaderMap_(values[0]);
  const idCol = headers["id"];
  if (idCol === undefined) return false;
  for (let r = 1; r < values.length; r++) {
    if (String(values[r][idCol] || "").trim() === String(id || "").trim()) {
      sheet.deleteRow(r + 1);
      return true;
    }
  }
  return false;
}

function readSheetObjects_(sheet) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];
  const headers = values[0].map(function(h) { return String(h || "").trim(); });
  return values.slice(1).map(function(row) {
    const item = {};
    headers.forEach(function(h, i) { item[h] = row[i]; });
    return item;
  });
}

function parseDateValue_(value) {
  if (!value) return 0;
  if (Object.prototype.toString.call(value) === "[object Date]") return value.getTime();
  const parsed = new Date(value);
  return isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}

function formatDateValue_(value) {
  const ts = parseDateValue_(value);
  if (!ts) return "";
  return Utilities.formatDate(new Date(ts), "Asia/Jakarta", "yyyy-MM-dd'T'HH:mm:ssXXX");
}

function safeErrorMessage_(error, fallback) {
  return (error && error.message) ? error.message : fallback;
}

function normalizePublicUrl_(url) {
  const trimmed = String(url || "").trim();
  if (!trimmed) return "";
  if (trimmed.indexOf("http://") === 0) return "https://" + trimmed.substring(7);
  return trimmed;
}

function jsonOutput_(payload) {
  return ContentService.createTextOutput(JSON.stringify(payload)).setMimeType(ContentService.MimeType.JSON);
}

