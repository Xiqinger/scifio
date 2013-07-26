/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif;

import io.scif.codec.CodecOptions;
import io.scif.common.DataTools;
import io.scif.io.RandomAccessOutputStream;
import io.scif.util.FormatTools;
import io.scif.util.SCIFIOMetadataTools;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;

import net.imglib2.meta.Axes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract superclass of all SCIFIO {@link io.scif.Writer} implementations.
 * 
 * @see io.scif.Writer
 * @see io.scif.HasFormat
 * @see io.scif.Metadata
 *
 * @author Mark Hiner
 * 
 * @param <M> - The Metadata type required by this Writer.
 */
public abstract class AbstractWriter<M extends TypedMetadata>
  extends AbstractHasSource implements TypedWriter<M> {

  // -- Constants --

  protected static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

  // -- Fields --

  /** Metadata, of the output type, describing the input source. */
  protected M metadata;

  /** Frame rate to use when writing in frames per second, if applicable. */
  protected int fps = 10;

  /** Default color model. */
  protected ColorModel cm;

  /** Available compression types. */
  protected String[] compressionTypes;

  /** Current compression type. */
  protected String compression;

  /** The options if required. */
  protected CodecOptions options;

  /**
   * Whether each plane in each image of the current file has been
   * prepped for writing.
   */
  protected boolean[][] initialized;

  /** Whether the channels in an RGB image are interleaved. */
  protected boolean interleaved;

  /** The number of valid bits per pixel. */
  protected int validBits;

  /** Whether or not we are writing planes sequentially. */
  protected boolean sequential;

  /** Where the image should be written. */
  protected RandomAccessOutputStream out;

  // -- Writer API Methods --
  
  /* @see io.scif.Writer#savePlane(int, int, Object) */
  public void savePlane(final int imageIndex, final int planeIndex,
    final Plane plane) throws FormatException, IOException
  {
    final int width = metadata.getAxisLength(imageIndex, Axes.X);
    final int height = metadata.getAxisLength(imageIndex, Axes.Y);
    savePlane(imageIndex, planeIndex, plane, 0, 0, width, height);
  }
  
  /* @see io.scif.Writer#canDoStacks() */
  public boolean canDoStacks() {
    return false;
  }
  
  /*
   * @see io.scif.Writer#setMetadata(io.scif.Metadata)
   */
  public void setMetadata(Metadata meta) throws FormatException {
    setMetadata(SCIFIOMetadataTools.<M>castMeta(meta));
  }

  /* @see io.scif.Writer#getMetadata() */
  public M getMetadata() {
    return metadata;
  }

  /* @see io.scif.Writer#setStream(String) */
  public void setDest(final String fileName)
    throws FormatException, IOException
  {
    setDest(fileName, 0);
  }

  /* @see io.scif.Writer#setStream(File) */
  public void setDest(final File file) throws FormatException, IOException {
    setDest(file.getName(), 0);
  }

  /* @see io.scif.Writer#setStream(RandomAccessOutputStream) */
  public void setDest(final RandomAccessOutputStream out)
    throws FormatException, IOException
  {
    setDest(out, 0);
  }
  
  /* @see io.scif.Writer#setStream(String, int) */
  public void setDest(final String fileName, final int imageIndex)
    throws FormatException, IOException
  {
    getMetadata().setDatasetName(fileName);
    setDest(new RandomAccessOutputStream(getContext(), fileName));
  }

  /* @see io.scif.Writer#setStream(File, int) */

  public void setDest(final File file, final int imageIndex)
    throws FormatException, IOException
  {
    setDest(file.getName());
  }

  /* @see io.scif.Writer#setStream(RandomAccessOutputStream, int) */
  public void setDest(final RandomAccessOutputStream out, final int imageIndex)
    throws FormatException, IOException
  {
    if (metadata == null)
      throw new FormatException(
        "Can not set Destination without setting Metadata first.");

    //FIXME
    // set metadata.datasetName here when RAOS has better id handling
    
    this.out = out;
    initialize(imageIndex);
  }

  /* @see io.scif.Writer#getStream() */
  public RandomAccessOutputStream getStream() {
    return out;
  }

  /* @see io.scif.Writer#setColorModel(ColorModel) */
  public void setColorModel(final ColorModel cm) {
    this.cm = cm;
  }

  /* @see io.scif.Writer#getColorModel() */
  public ColorModel getColorModel() {
    return cm;
  }

  /* @see io.scif.Writer#setFramesPerSecond(int) */
  public void setFramesPerSecond(final int rate) {
    fps = rate;
  }

  /* @see io.scif.Writer#getFramesPerSecond() */
  public int getFramesPerSecond() {
    return fps;
  }

  /* @see io.scif.Writer#getCompressionTypes() */
  public String[] getCompressionTypes() {
    return compressionTypes;
  }

  /* @see io.scif.Writer#getPixelTypes() */
  public int[] getPixelTypes() {
    return getPixelTypes(getCompression());
  }

  /* @see io.scif.Writer#getPixelTypes(String) */
  public int[] getPixelTypes(final String codec) {
    return new int[] {
        FormatTools.INT8, FormatTools.UINT8, FormatTools.INT16,
        FormatTools.UINT16, FormatTools.INT32, FormatTools.UINT32,
        FormatTools.FLOAT};
  }

  /* @see io.scif.Writer#isSupportedType(int) */
  public boolean isSupportedType(final int type) {
    final int[] types = getPixelTypes();
    for (int i = 0; i < types.length; i++) {
      if (type == types[i]) return true;
    }
    return false;
  }

  /* @see io.scif.Writer#setCompression(String) */
  public void setCompression(final String compress) throws FormatException {
    for (int i = 0; i < compressionTypes.length; i++) {
      if (compressionTypes[i].equals(compress)) {
        compression = compress;
        return;
      }
    }
    throw new FormatException("Invalid compression type: " + compress);
  }

  /* @see io.scif.Writer#getCompression() */
  public String getCompression() {
    return compression;
  }


  /* @see io.scif.Writer#setCodecOptions(CodecOptions) */
  public void setCodecOptions(final CodecOptions options) {
    this.options = options;
  }
  /* @see io.scif.Writer#changeOutputFile(String) */
  public void changeOutputFile(final String id)
    throws FormatException, IOException
  {
    setDest(id);
  }

  /* @see io.scif.Writer#setWriterSequentially(boolean) */
  public void setWriteSequentially(final boolean sequential) {
    this.sequential = sequential;
  }
  
  // -- TypedWriter API Methods --

  /*
   * @see io.scif.TypedWriter#setMetadata(io.scif.TypedMetadata)
   */
  public void setMetadata(final M meta) throws FormatException {
    if (metadata != null && metadata != meta) {
      try {
        metadata.close();
      } catch (IOException e) {
        throw new FormatException(e);
      }
    }
    
    if (out != null) {
      try {
        close();
      } catch (IOException e) {
        throw new FormatException(e);
      }
    }
    
    metadata = meta;
  }
  
  // -- HasSource API Methods --
  
  /*
   * @see io.scif.HasSource#close(boolean)
   */
  public void close(boolean fileOnly) throws IOException {
    if (out != null) out.close();
    if (metadata != null) metadata.close(fileOnly);
    out = null;
    initialized = null;
  }
  
  // -- Helper methods --

  /** Sets up the initialized array and ensures this Writer is ready for writing */
  private void initialize(final int imageIndex)
    throws FormatException, IOException
  {
    SCIFIOMetadataTools.verifyMinimumPopulated(metadata, out);
    initialized = new boolean[metadata.getImageCount()][];
    for (int i = 0; i < metadata.getImageCount(); i++) {
      initialized[i] = new boolean[getPlaneCount(i)];
    }
  }

  /** Retrieve the total number of planes in the current series. */
  protected int getPlaneCount(final int imageIndex) {
    final int z = metadata.getAxisLength(imageIndex, Axes.Z);
    final int t = metadata.getAxisLength(imageIndex, Axes.TIME);
    final int c = metadata.getEffectiveSizeC(imageIndex);
    return z * c * t;
  }

  /**
   * Returns true if the given rectangle coordinates correspond to a full
   * image in the given series.
   */
  protected boolean isFullPlane(final int imageIndex, final int x, final int y,
    final int w, final int h)
  {
    final int sizeX = metadata.getAxisLength(imageIndex, Axes.X);
    final int sizeY = metadata.getAxisLength(imageIndex, Axes.Y);
    return x == 0 && y == 0 && w == sizeX && h == sizeY;
  }

  /**
   * Ensure that the arguments that are being passed to saveBytes(...) are
   * valid.
   * @throws FormatException if any of the arguments is invalid.
   */
  protected void checkParams(final int imageIndex, final int planeIndex,
    final byte[] buf, final int x, final int y, final int w, final int h)
    throws FormatException
  {
    SCIFIOMetadataTools.verifyMinimumPopulated(metadata, out, imageIndex, planeIndex);

    if (buf == null) throw new FormatException("Buffer cannot be null.");
    final int z = metadata.getAxisLength(imageIndex, Axes.Z);
    final int t = metadata.getAxisLength(imageIndex, Axes.TIME);
    final int c = metadata.getAxisLength(imageIndex, Axes.CHANNEL);
    final int planes = z * c * t;

    if (planeIndex < 0)
      throw new FormatException(String.format(
        "Plane index:%d must be >= 0", planeIndex));
    if (planeIndex >= planes) {
      throw new FormatException(String.format(
        "Plane index:%d must be < %d", planeIndex, planes));
    }

    final int sizeX = metadata.getAxisLength(imageIndex, Axes.X);
    final int sizeY = metadata.getAxisLength(imageIndex, Axes.Y);
    if (x < 0)
      throw new FormatException(String.format("X:%d must be >= 0", x));
    if (y < 0)
      throw new FormatException(String.format("Y:%d must be >= 0", y));
    if (x >= sizeX) {
      throw new FormatException(String.format("X:%d must be < %d", x, sizeX));
    }
    if (y >= sizeY) {
      throw new FormatException(String.format("Y:%d must be < %d", y, sizeY));
    }
    if (w <= 0)
      throw new FormatException(String.format("Width:%d must be > 0", w));
    if (h <= 0)
      throw new FormatException(String.format("Height:%d must be > 0", h));
    if (x + w > sizeX)
      throw new FormatException(String.format(
        "(w:%d + x:%d) must be <= %d", w, x, sizeX));
    if (y + h > sizeY)
      throw new FormatException(String.format(
        "(h:%d + y:%d) must be <= %d", h, y, sizeY));

    final int pixelType = metadata.getPixelType(imageIndex);
    final int bpp = FormatTools.getBytesPerPixel(pixelType);
    int samples = metadata.getRGBChannelCount(imageIndex);
    if (samples == 0) samples = 1;
    final int minSize = bpp * w * h * samples;
    if (buf.length < minSize) {
      throw new FormatException("Buffer is too small; expected " + minSize +
        " bytes, got " + buf.length + " bytes.");
    }

    if (!DataTools.containsValue(getPixelTypes(compression), pixelType)) {
      throw new FormatException("Unsupported image type '" +
        FormatTools.getPixelTypeString(pixelType) + "'.");
    }
  }
}