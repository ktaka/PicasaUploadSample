/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


package org.ktaka.api.services.picasa;

import android.util.Log;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.xml.atom.AtomContent;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.atom.Atom;

import java.io.IOException;
import java.util.Arrays;

import org.ktaka.api.services.picasa.model.AlbumEntry;
import org.ktaka.api.services.picasa.model.AlbumFeed;
import org.ktaka.api.services.picasa.model.Entry;
import org.ktaka.api.services.picasa.model.Feed;
import org.ktaka.api.services.picasa.model.GeorssWhere;
import org.ktaka.api.services.picasa.model.GmlPoint;
import org.ktaka.api.services.picasa.model.PhotoEntry;
import org.ktaka.api.services.picasa.model.TagEntry;
import org.ktaka.api.services.picasa.model.UserFeed;
import org.ktaka.api.services.samples.shared.gdata.xml.GDataXmlClient;

/**
 * Client for the Picasa Web Albums Data API.
 *
 * @author Yaniv Inbar
 */
public final class PicasaClient extends GDataXmlClient {

  static final XmlNamespaceDictionary DICTIONARY =
      new XmlNamespaceDictionary().set("", "http://www.w3.org/2005/Atom")
          .set("exif", "http://schemas.google.com/photos/exif/2007")
          .set("gd", "http://schemas.google.com/g/2005")
          .set("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
          .set("georss", "http://www.georss.org/georss")
          .set("gml", "http://www.opengis.net/gml")
          .set("gphoto", "http://schemas.google.com/photos/2007")
          .set("media", "http://search.yahoo.com/mrss/")
          .set("openSearch", "http://a9.com/-/spec/opensearch/1.1/")
          .set("xml", "http://www.w3.org/XML/1998/namespace");

  public PicasaClient(HttpRequestFactory requestFactory) {
    super("2", requestFactory, DICTIONARY);
  }

  public void executeDelete(Entry entry) throws IOException {
    PicasaUrl url = new PicasaUrl(entry.getEditLink());
    super.executeDelete(url, entry.etag);
  }

  <T> T executeGet(PicasaUrl url, Class<T> parseAsType) throws IOException {
    return super.executeGet(url, parseAsType);
  }

  public <T extends Entry> T executePatchRelativeToOriginal(T original, T updated)
      throws IOException {
    PicasaUrl url = new PicasaUrl(updated.getEditLink());
    return super.executePatchRelativeToOriginal(url, original, updated, original.etag);
  }

  <T> T executePost(PicasaUrl url, T content) throws IOException {
    return super.executePost(url, content instanceof Feed, content);
  }

  public AlbumEntry executeGetAlbum(String link) throws IOException {
    PicasaUrl url = new PicasaUrl(link);
    return executeGet(url, AlbumEntry.class);
  }

  public <T extends Entry> T executeInsert(PicasaUrl url, T entry) throws IOException {
    return executePost(url, entry);
  }

  public <T extends Entry> T executeInsert(Feed feed, T entry) throws IOException {
    return executeInsert(new PicasaUrl(feed.getPostLink()), entry);
  }

  public AlbumFeed executeGetAlbumFeed(PicasaUrl url) throws IOException {
    url.kinds = "photo";
    url.maxResults = 5;
    return executeGet(url, AlbumFeed.class);
  }

  public UserFeed executeGetUserFeed(PicasaUrl url) throws IOException {
    url.kinds = "album";
    url.maxResults = 3;
    return executeGet(url, UserFeed.class);
  }

  public PhotoEntry executeInsertPhotoEntry(
      PicasaUrl albumFeedUrl, InputStreamContent content, String fileName) throws IOException {
    HttpRequest request = getRequestFactory().buildPostRequest(albumFeedUrl, content);
    HttpHeaders headers = new HttpHeaders();
    Atom.setSlugHeader(headers, fileName);
    request.setHeaders(headers);
    return execute(request).parseAs(PhotoEntry.class);
  }

  public PhotoEntry executeInsertPhotoEntryWithMetadata(
      PhotoEntry photo, PicasaUrl albumFeedUrl, InputStreamContent content, GmlPoint point)
      throws IOException {
	GeorssWhere georss = new GeorssWhere();
	georss.point = point;
	photo.georssWhere = georss;
	HttpRequest request = getRequestFactory().buildPostRequest(albumFeedUrl, null);
    AtomContent atomContent = AtomContent.forEntry(DICTIONARY, photo);
    request.setContent(new MultipartContent().setContentParts(Arrays.asList(atomContent, content)));
    request.getHeaders().setMimeVersion("1.0");
    return execute(request).parseAs(PhotoEntry.class);
  }
  
  public void executeAddTagToPhoto(PicasaUrl feedUrl, TagEntry tag) throws IOException {
	  AtomContent atomContent = AtomContent.forEntry(DICTIONARY, tag);
	  HttpRequest request = getRequestFactory().buildPostRequest(feedUrl, null);
	  request.setContent(atomContent);
	  execute(request);
  }
  
}
